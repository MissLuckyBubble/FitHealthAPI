package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.ForbiddenException;
import fit.health.fithealthapi.exceptions.InvalidRequestException;
import fit.health.fithealthapi.exceptions.MealPlanNotFoundException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.repository.DiaryEntryRepository;
import fit.health.fithealthapi.utils.MealContainerUtils;
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
        if (diaryEntryRepository.findByOwnerAndDate(user, date).isPresent()) {
            throw new InvalidRequestException("A diary entry for this date already exists.");
        }

        DiaryEntry newEntry = new DiaryEntry();
        newEntry.setOwner(user);
        newEntry.setDate(date);
        newEntry.setDailyCalorieGoal(user.getDailyCalorieGoal());
        calculate(newEntry);
        return diaryEntryRepository.save(newEntry);
    }

    public Optional<DiaryEntry> getDiaryEntry(User user, LocalDate date) {
        return diaryEntryRepository.findByOwnerAndDate(user, date);
    }

    public List<DiaryEntry> getOwnerDiaryEntries(User user) {
        return diaryEntryRepository.findByOwner(user);
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

        Optional<DiaryEntry> optionalDiaryEntry = dto.getDiaryEntryId()!= null ?  diaryEntryRepository.findById(dto.getDiaryEntryId()) : Optional.empty();
        DiaryEntry diaryEntry = new DiaryEntry();
        if (optionalDiaryEntry.isEmpty()) {
            Optional<DiaryEntry> optional = diaryEntryRepository.findByOwnerAndDate(user,dto.getDate()!=null ? dto.getDate() : LocalDate.now());
            if (optional.isPresent()) {
                diaryEntry = optional.get();
            }else {
                diaryEntry.setOwner(user);
                diaryEntry.setDate(dto.getDate()!=null ? dto.getDate() : LocalDate.now());
                diaryEntry.setDailyCalorieGoal(user.getDailyCalorieGoal());
            }
        }
        else {
            diaryEntry = optionalDiaryEntry.get();
        }

        Meal meal = mealService.processMealRequest(dto, user);

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
        if (!diaryEntry.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not allowed to remove this meal.");
        }
        MealContainerUtils.removeMealByType(diaryEntry, recipeType);
        calculate(diaryEntry);
        return diaryEntryRepository.save(diaryEntry);
    }

    public void calculate(DiaryEntry diaryEntry) {
        if (diaryEntry.getOwner() != null) {
            diaryEntry.setDailyCalorieGoal(diaryEntry.getOwner().getDailyCalorieGoal());
        }

        MealContainerUtils.updateMealContainerData(diaryEntry);
    }

}
