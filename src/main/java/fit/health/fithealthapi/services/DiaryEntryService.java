package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.ForbiddenException;
import fit.health.fithealthapi.exceptions.InvalidRequestException;
import fit.health.fithealthapi.exceptions.MealPlanNotFoundException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.repository.DiaryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiaryEntryService {
    private final DiaryEntryRepository diaryEntryRepository;
    private final MealService mealService;

    public DiaryEntry createDiaryEntry(User user, LocalDate date) {
        if (diaryEntryRepository.findByUserAndDate(user, date).isPresent()) {
            throw new InvalidRequestException("A diary entry for this date already exists.");
        }

        DiaryEntry newEntry = new DiaryEntry();
        newEntry.setUser(user);
        newEntry.setDate(date);
        newEntry.setDailyCalorieGoal(user.getDailyCalorieGoal());
        calculate(newEntry);
        return diaryEntryRepository.save(newEntry);
    }

    public Optional<DiaryEntry> getDiaryEntry(User user, LocalDate date) {
        return diaryEntryRepository.findByUserAndDate(user, date);
    }

    public List<DiaryEntry> getUserDiaryEntries(User user) {
        return diaryEntryRepository.findByUser(user);
    }

    public DiaryEntry updateDiaryEntry(DiaryEntry updatedEntry) {
        if (!diaryEntryRepository.existsById(updatedEntry.getId())) {
            throw new NotFoundException("Diary entry does not exist.");
        }
        calculate(updatedEntry);
        return diaryEntryRepository.save(updatedEntry);
    }

    public void deleteDiaryEntry(Long id) {
        if (!diaryEntryRepository.existsById(id)) {
            throw new NotFoundException("Diary entry not found.");
        }
        diaryEntryRepository.deleteById(id);
    }

    @Transactional
    public DiaryEntry assignMealToDiary(CreateMealRequestDto dto, User user) {

        // ✅ Check if diary entry exists, or create a new one
        Optional<DiaryEntry> optionalDiaryEntry = dto.getDiaryEntryId()!= null ?  diaryEntryRepository.findById(dto.getDiaryEntryId()) : Optional.empty();
        DiaryEntry diaryEntry = new DiaryEntry();
        if (optionalDiaryEntry.isEmpty()) {
            Optional<DiaryEntry> optional = diaryEntryRepository.findByUserAndDate(user,dto.getDate()!=null ? dto.getDate() : LocalDate.now());
            if (optional.isPresent()) {
                diaryEntry = optional.get();
            }else {
                diaryEntry.setUser(user);
                diaryEntry.setDate(dto.getDate()!=null ? dto.getDate() : LocalDate.now());
                diaryEntry.setDailyCalorieGoal(user.getDailyCalorieGoal());
            }
        }
        else {
            diaryEntry = optionalDiaryEntry.get();
        }

        // ✅ Process Meal (either modify existing or create new)
        Meal meal = mealService.processMealRequest(dto, user);

        // ✅ Assign meal to the correct type
        switch (dto.getRecipeType()) {
            case BREAKFAST -> diaryEntry.setBreakfast(meal);
            case LUNCH -> diaryEntry.setLunch(meal);
            case DINNER -> diaryEntry.setDinner(meal);
            case SNACK -> diaryEntry.setSnack(meal);
        }
        calculate(diaryEntry);
        return  diaryEntryRepository.save(diaryEntry);
    }

    public DiaryEntry removeMeal(Long dairyId, RecipeType recipeType, User user) {
        DiaryEntry diaryEntry = diaryEntryRepository.findById(dairyId).orElseThrow(()->new MealPlanNotFoundException("Dairy not found."));
        if (!diaryEntry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not allowed to remove this meal.");
        } else {
            switch (recipeType) {
                case BREAKFAST -> diaryEntry.setBreakfast(null);
                case LUNCH -> diaryEntry.setLunch(null);
                case DINNER -> diaryEntry.setDinner(null);
                case SNACK -> diaryEntry.setSnack(null);
                default -> throw new IllegalStateException("Unexpected value: " + recipeType);
            }
            calculate(diaryEntry);
            return diaryEntryRepository.save(diaryEntry);
        }
    }

    @Transactional
    public void calculate(DiaryEntry diaryEntry) {
        if (diaryEntry.getUser() != null) {
            diaryEntry.setDailyCalorieGoal(diaryEntry.getUser().getDailyCalorieGoal());
        }

        if (diaryEntry.getMacronutrients() == null) {
            diaryEntry.setMacronutrients(new Macronutrients());
        }

        diaryEntry.getMacronutrients().reset();

        if (diaryEntry.getBreakfast() != null && diaryEntry.getBreakfast().getMacronutrients() != null) {
            diaryEntry.getMacronutrients().add(diaryEntry.getBreakfast().getMacronutrients());
        }
        if (diaryEntry.getLunch() != null && diaryEntry.getLunch().getMacronutrients() != null) {
            diaryEntry.getMacronutrients().add(diaryEntry.getLunch().getMacronutrients());
        }
        if (diaryEntry.getDinner() != null && diaryEntry.getDinner().getMacronutrients() != null) {
            diaryEntry.getMacronutrients().add(diaryEntry.getDinner().getMacronutrients());
        }
        if (diaryEntry.getSnack() != null && diaryEntry.getSnack().getMacronutrients() != null) {
            diaryEntry.getMacronutrients().add(diaryEntry.getSnack().getMacronutrients());
        }
    }
}
