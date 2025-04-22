package fit.health.fithealthapi.services;

import fit.health.fithealthapi.exceptions.ForbiddenException;
import fit.health.fithealthapi.exceptions.InvalidRequestException;
import fit.health.fithealthapi.exceptions.MealPlanNotFoundException;
import fit.health.fithealthapi.exceptions.NotFoundException;
import fit.health.fithealthapi.model.*;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.repository.DiaryEntryRepository;
import fit.health.fithealthapi.repository.MealItemRepository;
import fit.health.fithealthapi.repository.MealPlanRepository;
import fit.health.fithealthapi.repository.MealRepository;
import fit.health.fithealthapi.utils.MealContainerUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiaryEntryService {
    private final DiaryEntryRepository diaryEntryRepository;
    private final MealService mealService;
    private final MealItemRepository mealItemRepository;
    private final MealRepository mealRepository;
    private final MealPlanRepository mealPlanRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public DiaryEntry createDiaryEntry(User user, LocalDate date) {
        if (diaryEntryRepository.findByOwnerIdAndDate(user.getId(), date).isPresent()) {
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
        return diaryEntryRepository.findByOwnerIdAndDate(user.getId(), date);
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

        try {
            Optional<DiaryEntry> optionalDiaryEntry = dto.getDiaryEntryId() != null ? diaryEntryRepository.findById(dto.getDiaryEntryId()) : Optional.empty();
            DiaryEntry diaryEntry = new DiaryEntry();

            if (optionalDiaryEntry.isEmpty()) {
                Optional<DiaryEntry> optional = diaryEntryRepository.findByOwnerIdAndDate(user.getId(), dto.getDate() != null ? dto.getDate() : LocalDate.now());
                if (optional.isPresent()) {
                    diaryEntry = optional.get();
                } else {
                    diaryEntry.setOwner(user);
                    diaryEntry.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
                    diaryEntry.setDailyCalorieGoal(user.getDailyCalorieGoal());
                }
            } else {
                diaryEntry = optionalDiaryEntry.get();
            }

            Meal meal = MealContainerUtils.ensureMealSlot(diaryEntry, dto.getRecipeType(), () -> createNewMeal(dto, user));

            if (dto.getMealId() != null) {
                Meal original = mealRepository.findById(dto.getMealId())
                        .orElseThrow(() -> new NotFoundException("Original meal not found"));
                MealContainerUtils.copyMealItemsSafeAppend(original, meal, user, mealItemRepository, dto.getQuantity(),entityManager);
            } else {
                mealService.addMealItemToMeal(dto, meal, user);
            }

            MealContainerUtils.updateMealContainerData(diaryEntry);

            calculate(diaryEntry);
            return diaryEntryRepository.save(diaryEntry);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private Meal createNewMeal(CreateMealRequestDto dto, User user) {
        Meal meal = new Meal();
        meal.setName(dto.getMealName() != null ? dto.getMealName() : "");
        meal.setOwner(user);
        if (dto.getRecipeType() != null) {
            meal.getRecipeTypes().add(dto.getRecipeType());
        }
        meal.setMacronutrients(new Macronutrients());
        return mealRepository.save(meal);
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

    public void removeMealItem(Long mealItemId, User user){
        Meal meal = mealService.removeMealItem(mealItemId,user);
        for(DiaryEntry diaryEntry : diaryEntryRepository.findByAnyMeal(meal)){
            calculate(diaryEntry);
            diaryEntryRepository.save(diaryEntry);
        }
    }

    public void calculate(DiaryEntry diaryEntry) {
        if (diaryEntry.getOwner() != null) {
            diaryEntry.setDailyCalorieGoal(diaryEntry.getOwner().getDailyCalorieGoal());
        }

        MealContainerUtils.updateMealContainerData(diaryEntry);
    }

    @Transactional
    public void copyMealPlanToDiary(Long mealPlanId, LocalDate date, User user) {
        MealPlan mealPlan = mealPlanRepository.findWithMealsById(mealPlanId)
                .orElseThrow(() -> new NotFoundException("Meal Plan not found"));

        Optional<DiaryEntry> optionalDiaryEntry = diaryEntryRepository
                .findByOwnerIdAndDate(user.getId(), date);
        DiaryEntry diary;
        if (optionalDiaryEntry.isPresent()) {
            diary = optionalDiaryEntry.get();
        }else {
            diary = new DiaryEntry();
            diary.setOwner(user);
            diary.setDate(date);
            diary.setDailyCalorieGoal(user.getDailyCalorieGoal());
            diary.setMacronutrients(new Macronutrients());
            try {
                diaryEntryRepository.save(diary);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        for (RecipeType type : RecipeType.values()) {
            Meal meal = MealContainerUtils.getMealByType(mealPlan, type);
            if (meal != null) {
                Meal targetMeal = MealContainerUtils.ensureMealSlot(diary, type, () -> {
                    Meal newMeal = new Meal();
                    newMeal.setOwner(user);
                    newMeal.setName(meal.getName());
                    newMeal.setRecipeTypes(Set.of(type));
                    newMeal.setMacronutrients(new Macronutrients());
                    return mealRepository.save(newMeal);
                });

                try {
                    MealContainerUtils.copyMealItemsSafeAppend(
                            meal,
                            targetMeal,
                            user,
                            mealItemRepository,
                            1f,
                            entityManager
                    );
                }catch (Exception e){
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        entityManager.flush();

        MealContainerUtils.updateMealContainerData(diary);
        calculate(diary);
        diaryEntryRepository.save(diary);
    }
}
