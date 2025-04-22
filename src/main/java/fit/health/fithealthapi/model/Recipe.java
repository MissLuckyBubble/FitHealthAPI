package fit.health.fithealthapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fit.health.fithealthapi.model.enums.RecipeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipes")
public class Recipe extends MealComponent{

    private String description;

    @Column(nullable = false)
    private Integer preparationTime; // in minutes

    @Column(nullable = false)
    private Integer cookingTime; // in minutes

    @Column(nullable = false)
    private Integer servingSize;

    private Float totalWeight;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RecipeTypeWrapper> recipeTypeWrappers = new HashSet<>();

    @Transient
    public Set<RecipeType> getRecipeTypes() {
        return recipeTypeWrappers.stream()
                .map(RecipeTypeWrapper::getType)
                .collect(Collectors.toSet());
    }

    public void setRecipeTypes(Set<RecipeType> recipeTypes) {
        this.recipeTypeWrappers.clear();
        if (recipeTypes != null) {
            for (RecipeType type : recipeTypes) {
                RecipeTypeWrapper wrapper = new RecipeTypeWrapper();
                wrapper.setType(type);
                wrapper.setRecipe(this);
                this.recipeTypeWrappers.add(wrapper);
            }
        }
    }

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecipeIngredient> ingredients = new HashSet<>();

    public void setIngredients(Set<RecipeIngredient> ingredients) {
        this.ingredients.clear();
        if(ingredients != null) {
            for(RecipeIngredient ingredient : ingredients) {
                ingredient.setRecipe(this);
            }
            this.ingredients.addAll(ingredients);
        }
    }

    public void checkAndUpdateVerification() {
        boolean allVerified = ingredients.stream()
                .allMatch(ingredient -> ingredient.getFoodItem().isVerifiedByAdmin());

        if (this.verifiedByAdmin != allVerified) {
            this.verifiedByAdmin = allVerified;
        }
    }
}
