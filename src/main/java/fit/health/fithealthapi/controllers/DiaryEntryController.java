package fit.health.fithealthapi.controllers;

import fit.health.fithealthapi.model.DiaryEntry;
import fit.health.fithealthapi.model.User;
import fit.health.fithealthapi.model.dto.CreateMealRequestDto;
import fit.health.fithealthapi.model.enums.RecipeType;
import fit.health.fithealthapi.services.DiaryEntryService;
import fit.health.fithealthapi.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/diary-entries")
@RequiredArgsConstructor
public class DiaryEntryController {
    private final DiaryEntryService diaryEntryService;
    private final UserService userService;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByUsername(username);
    }

    @PostMapping
    public ResponseEntity<?> createDiaryEntry(@RequestParam LocalDate date) {
        User user = getAuthenticatedUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user");

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(diaryEntryService.createDiaryEntry(user, date));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{date}")
    public ResponseEntity<?> getDiaryEntry(@PathVariable LocalDate date) {
        User user = getAuthenticatedUser();
        return diaryEntryService.getDiaryEntry(user, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping
    public ResponseEntity<List<DiaryEntry>> getUserDiaryEntries() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(diaryEntryService.getOwnerDiaryEntries(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDiaryEntry(@PathVariable Long id, @RequestBody DiaryEntry diaryEntry) {
        diaryEntry.setId(id);
        return ResponseEntity.ok(diaryEntryService.updateDiaryEntry(diaryEntry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiaryEntry(@PathVariable Long id) {
        diaryEntryService.deleteDiaryEntry(id);
        return ResponseEntity.ok("Diary entry deleted successfully");
    }

    @PostMapping("/assign-meal")
    public ResponseEntity<?> assignMeal(@RequestBody CreateMealRequestDto dto) {
        User user = getAuthenticatedUser();

        return ResponseEntity.ok(diaryEntryService.assignMealToDiary(dto, user));
    }

    @DeleteMapping("mealItem/{id}")
    public ResponseEntity<?> removeMealItem(@PathVariable Long id){
        User user = getAuthenticatedUser();
        diaryEntryService.removeMealItem(id, user);
        return ResponseEntity.ok("Meal Item successfully removed");
    }

    @PutMapping("{id}/meals/{recipeType}")
    public ResponseEntity<?> updateMeal(@PathVariable Long id, @PathVariable String recipeType) {
        User user = getAuthenticatedUser();
        DiaryEntry diaryEntry = diaryEntryService.removeMeal(id, RecipeType.fromString(recipeType), user);
        return ResponseEntity.ok(diaryEntry);
    }
}

