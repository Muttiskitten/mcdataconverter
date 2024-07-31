package dataconverter;

import org.geysermc.geyser.api.plugin.GeyserPlugin;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.recipe.Ingredient;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.recipe.RecipeType;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapedRecipeData;
import com.github.steveice10.mc.protocol.data.game.recipe.data.ShapelessRecipeData;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.recipe.RecipeJsonSerializer;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.registry.BlockRegistries;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.sponge.GeyserSpongePlugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class convertermc extends GeyserPlugin {
    public class BedrockConversionPlugin extends GeyserSpongePlugin {
        private static final Map<String, ItemEntry> customItems = new HashMap<>();
        private static final Map<String, Recipe> customRecipes = new HashMap<>();

        @Override
        public void onEnable() {
            super.onEnable();
            scanDatapacks();
            registerCustomContent();
        }

        private void scanDatapacks() {
            // Scan all datapacks in the server's datapack folder
            // and extract custom items and recipes
            for (String datapackName : getDatapackNames()) {
                scanDatapack(datapackName);
            }
        }

        private void scanDatapack(String datapackName) {
            // Implement logic to scan the specific datapack
            // and extract custom items and recipes
            for (ItemStack customItem : getCustomItems(datapackName)) {
                registerCustomItem(customItem);
            }
            for (Recipe customRecipe : getCustomRecipes(datapackName)) {
                registerCustomRecipe(customRecipe);
            }
        }

        private void registerCustomContent() {
            // Register custom items with Geyser
            for (Map.Entry<String, ItemEntry> entry : customItems.entrySet()) {
                String itemId = entry.getKey();
                ItemEntry itemEntry = entry.getValue();
                ItemRegistry.ITEMS.registerItem(itemId, itemEntry);
            }

            // Register custom recipes with Geyser
            for (Map.Entry<String, Recipe> entry : customRecipes.entrySet()) {
                String recipeId = entry.getKey();
                Recipe recipe = entry.getValue();
                RecipeJsonSerializer.registerRecipe(recipeId, recipe);
            }
        }

        private void registerCustomItem(ItemStack customItem) {
            // Convert the Java Edition item to a Bedrock Edition ItemData
            ItemData bedrockItem = convertItemToBedrock(customItem);

            // Create a new ItemEntry and register it with Geyser
            ItemEntry itemEntry = new ItemEntry(bedrockItem);
            customItems.put(customItem.getId(), itemEntry);
        }

        private void registerCustomRecipe(Recipe customRecipe) {
            // Convert the Java Edition recipe to a Bedrock Edition recipe
            Recipe bedrockRecipe = convertRecipeToBedrock(customRecipe);

            // Register the Bedrock Edition recipe with Geyser
            String recipeId = customRecipe.getId();
            customRecipes.put(recipeId, bedrockRecipe);
        }

        private ItemData convertItemToBedrock(ItemStack javaItem) {
            // Implement logic to convert the Java Edition item to a Bedrock Edition ItemData
            // This may involve looking up the corresponding Bedrock Edition item ID and creating the ItemData
            int bedrockItemId = lookupBedrockItemId(javaItem.getId());
            int bedrockItemData = lookupBedrockItemData(javaItem.getData());
            return ItemData.of(bedrockItemId, (byte) bedrockItemData, javaItem.getAmount());
        }

        private Recipe convertRecipeToBedrock(Recipe javaRecipe) {
            // Implement logic to convert the Java Edition recipe to a Bedrock Edition recipe
            // This may involve creating the appropriate RecipeType and recipe data
            if (javaRecipe.getType().equals("minecraft:crafting_shaped")) {
                return createShapedRecipe(javaRecipe);
            } else if (javaRecipe.getType().equals("minecraft:crafting_shapeless")) {
                return createShapelessRecipe(javaRecipe);
            }
            // Handle other types of recipes as needed
            return null;
        }

        private Recipe createShapedRecipe(Recipe shapedRecipe) {
            // Convert the Java Edition shaped recipe to a Bedrock Edition recipe
            int width = shapedRecipe.getPattern().get(0).length();
            int height = shapedRecipe.getPattern().size();
            Ingredient[] ingredients = new Ingredient[width * height];

            for (int i = 0; i < height; i++) {
                String row = shapedRecipe.getPattern().get(i);
                for (int j = 0; j < width; j++) {
                    char key = row.charAt(j);
                    ingredients[i * width + j] = shapedRecipe.getKey().get(String.valueOf(key));
                }
            }

            ItemStack output = shapedRecipe.getResult();
            return new Recipe(RecipeType.SHAPED, new ShapedRecipeData(width, height, ingredients, output));
        }

        private Recipe createShapelessRecipe(Recipe shapelessRecipe) {
            // Convert the Java Edition shapeless recipe to a Bedrock Edition recipe
            Ingredient[] ingredients = shapelessRecipe.getKey().values().toArray(new Ingredient[0]);
            ItemStack output = shapelessRecipe.getResult();
            return new Recipe(RecipeType.SHAPELESS, new ShapelessRecipeData(ingredients, output));
        }

        private int lookupBedrockItemId(String javaItemId) {
            // Implement logic to look up the corresponding Bedrock Edition item ID
            // based on the Java Edition item ID
            return BlockTranslator.getBlockStateId(BlockState.of(javaItemId));
        }

        private int lookupBedrockItemData(int javaItemData) {
            // Implement logic to look up the corresponding Bedrock Edition item data
            // based on the Java Edition item data
            return javaItemData;
        }

        private RecipeType getBedrockRecipeType(org.geysermc.mc.protocol.data.game.recipe.RecipeType javaRecipeType) {
            // Implement logic to map the Java Edition recipe type to a Bedrock Edition recipe type
            switch (javaRecipeType) {
                case SHAPED:
                    return RecipeType.SHAPED;
                case SHAPELESS:
                    return RecipeType.SHAPELESS;
                // Add more recipe type mappings as needed
                default:
                    return null;
            }
        }

        private List<String> getDatapackNames() {
            // Retrieve the list of datapack names from the world folder
            List<String> datapackNames = new ArrayList<>();
            File datapacksFolder = new File("path/to/your/world/datapacks"); // Adjust the path as needed
            File[] datapackFiles = datapacksFolder.listFiles();
            if (datapackFiles != null) {
                for (File datapackFile : datapackFiles) {
                    if (datapackFile.isDirectory() || datapackFile.getName().endsWith(".zip")) {
                        datapackNames.add(datapackFile.getName());
                    }
                }
            }
            return datapackNames;
        }

        private List<ItemStack> getCustomItems(String datapackName) {
            List<ItemStack> customItems = new ArrayList<>();
            File datapackFile = new File("path/to/your/world/datapacks/" + datapackName);
            try (ZipFile zipFile = new ZipFile(datapackFile)) {
                ZipEntry itemsEntry = zipFile.getEntry("data/your_namespace/recipes/custom_items.json"); // Adjust the path as needed
                if (itemsEntry != null) {
                    try (InputStream inputStream = zipFile.getInputStream(itemsEntry)) {
                        String jsonString = new String(inputStream.readAllBytes());
                        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                        Type listType = new TypeToken<ArrayList<ItemStack>>(){}.getType();
                        customItems = new Gson().fromJson(jsonObject.getAsJsonArray("items"), listType);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return customItems;
        }

        private List<Recipe> getCustomRecipes(String datapackName) {
            List<Recipe> customRecipes = new ArrayList<>();
            File datapackFile = new File("path/to/your/world/datapacks/" + datapackName);
            try (ZipFile zipFile = new ZipFile(datapackFile)) {
                ZipEntry recipesEntry = zipFile.getEntry("data/your_namespace/recipes/custom_recipes.json"); // Adjust the path as needed
                if (recipesEntry != null) {
                    try (InputStream inputStream = zipFile.getInputStream(recipesEntry)) {
                        String jsonString = new String(inputStream.readAllBytes());
                        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                        Type listType = new TypeToken<ArrayList<Recipe>>(){}.getType();
                        customRecipes = new Gson().fromJson(jsonObject.getAsJsonArray("recipes"), listType);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return customRecipes;
        }
    }
}
