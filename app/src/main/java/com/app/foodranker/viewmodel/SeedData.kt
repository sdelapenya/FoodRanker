package com.app.foodranker.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory

object SeedData {

    fun insertSeedPlates(onComplete: () -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        val plates = getSeedPlates()
        var completed = 0

        plates.forEach { plate ->
            db.collection("plates")
                .document(plate.id)
                .set(plate)
                .addOnSuccessListener {
                    completed++
                    if (completed == plates.size) onComplete()
                }
        }
    }

    private fun getSeedPlates(): List<Plate> = listOf(
        Plate(
            id = "plate_001",
            name = "Croquetas de Jamón Ibérico",
            description = "Cremosas por dentro, crujientes por fuera. Las mejores de Madrid",
            category = PlateCategory.TAPAS,
            restaurantName = "Bar El Brillante",
            restaurantAddress = "Glorieta Emperador Carlos V, 8",
            city = "Madrid", country = "España",
            imageUrl = "https://images.unsplash.com/photo-1515669097368-22e9427d24ab?w=800&q=80&fit=crop",
            averageScore = 9.2, totalRatings = 47,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709900000000
        ),
        Plate(
            id = "plate_002",
            name = "Ramen Tonkotsu",
            description = "Caldo de cerdo cremoso, fideos perfectos y huevo marinado",
            category = PlateCategory.RAMEN,
            restaurantName = "Noodle House Tokyo",
            restaurantAddress = "Calle Echegaray, 15",
            city = "Madrid", country = "España",
            imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&q=80&fit=crop",
            averageScore = 9.5, totalRatings = 83,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709800000000
        ),
        Plate(
            id = "plate_003",
            name = "Pizza Margherita DOC",
            description = "Masa napolitana, tomate San Marzano y mozzarella de búfala fresca",
            category = PlateCategory.PIZZA,
            restaurantName = "Pizzeria Brandi",
            restaurantAddress = "Salita Sant'Anna di Palazzo, 1",
            city = "Nápoles", country = "Italia",
            imageUrl = "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800&q=80&fit=crop",
            averageScore = 9.7, totalRatings = 124,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709700000000
        ),
        Plate(
            id = "plate_004",
            name = "Croissant au Beurre",
            description = "Hojaldrado, mantecoso y crujiente. Desayuno perfecto en París",
            category = PlateCategory.BREAKFAST,
            restaurantName = "Boulangerie Poilâne",
            restaurantAddress = "8 Rue du Cherche-Midi",
            city = "París", country = "Francia",
            imageUrl = "https://images.unsplash.com/photo-1555507036-ab1eebef9e4d?w=800&q=80&fit=crop",
            averageScore = 9.1, totalRatings = 56,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709600000000
        ),
        Plate(
            id = "plate_005",
            name = "Sushi Omakase",
            description = "12 piezas seleccionadas por el chef con pescado del día",
            category = PlateCategory.SUSHI,
            restaurantName = "Sushi Saito Barcelona",
            restaurantAddress = "Carrer de Muntaner, 60",
            city = "Barcelona", country = "España",
            imageUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=800&q=80&fit=crop",
            averageScore = 9.6, totalRatings = 91,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709500000000
        ),
        Plate(
            id = "plate_006",
            name = "Fish and Chips",
            description = "Bacalao fresco rebozado con chips caseras y salsa tártara",
            category = PlateCategory.SEAFOOD,
            restaurantName = "The Golden Hind",
            restaurantAddress = "73 Marylebone Lane",
            city = "Londres", country = "Reino Unido",
            imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&q=80&fit=crop",
            averageScore = 8.8, totalRatings = 38,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709400000000
        ),
        Plate(
            id = "plate_007",
            name = "Pastel de Nata",
            description = "Crema pastelera caramelizada en hojaldre crujiente, receta original",
            category = PlateCategory.DESSERT,
            restaurantName = "Pastéis de Belém",
            restaurantAddress = "Rua de Belém, 84-92",
            city = "Lisboa", country = "Portugal",
            imageUrl = "https://images.unsplash.com/photo-1551024709-8f23befc0c38?w=800&q=80&fit=crop",
            averageScore = 9.8, totalRatings = 215,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709300000000
        ),
        Plate(
            id = "plate_008",
            name = "Currywurst",
            description = "Salchicha con salsa de curry y ketchup, el bocado más berlinés",
            category = PlateCategory.OTHER,
            restaurantName = "Curry 36",
            restaurantAddress = "Mehringdamm 36",
            city = "Berlín", country = "Alemania",
            imageUrl = "https://images.unsplash.com/photo-1612392062631-94c9bfb1f7e6?w=800&q=80&fit=crop",
            averageScore = 8.6, totalRatings = 29,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709200000000
        ),
        Plate(
            id = "plate_009",
            name = "Hamburguesa Smash",
            description = "Doble smash de carne wagyu, queso americano y salsa secreta",
            category = PlateCategory.BURGER,
            restaurantName = "Goiko Madrid Centro",
            restaurantAddress = "Calle Fuencarral, 43",
            city = "Madrid", country = "España",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800&q=80&fit=crop",
            averageScore = 8.9, totalRatings = 67,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709100000000
        ),
        Plate(
            id = "plate_010",
            name = "Carbonara Originale",
            description = "Guanciale, pecorino romano, yema de huevo. Sin nata, como manda la tradición",
            category = PlateCategory.PASTA,
            restaurantName = "Trattoria da Enzo al 29",
            restaurantAddress = "Via dei Vascellari, 29",
            city = "Roma", country = "Italia",
            imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=800&q=80&fit=crop",
            averageScore = 9.4, totalRatings = 108,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1709000000000
        ),
        Plate(
            id = "plate_011",
            name = "Patatas Bravas",
            description = "Patatas fritas con salsa brava picante y alioli cremoso",
            category = PlateCategory.TAPAS,
            restaurantName = "Bar Tomás",
            restaurantAddress = "Carrer de la Legalitat, 22",
            city = "Barcelona", country = "España",
            imageUrl = "https://images.unsplash.com/photo-1518977676980-f19a16d6f6e8?w=800&q=80&fit=crop",
            averageScore = 9.0, totalRatings = 74,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1708900000000
        ),
        Plate(
            id = "plate_012",
            name = "Steak Frites",
            description = "Entrecot de buey con patatas fritas y mantequilla de hierbas",
            category = PlateCategory.STEAK,
            restaurantName = "Brasserie Lipp",
            restaurantAddress = "151 Boulevard Saint-Germain",
            city = "París", country = "Francia",
            imageUrl = "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800&q=80&fit=crop",
            averageScore = 9.3, totalRatings = 52,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1708800000000
        ),
        Plate(
            id = "plate_013",
            name = "Tiramisú Clásico",
            description = "Bizcochos empapados en espresso, mascarpone y cacao",
            category = PlateCategory.DESSERT,
            restaurantName = "Le Beccherie",
            restaurantAddress = "Piazza Ancilotto, 11",
            city = "Treviso", country = "Italia",
            imageUrl = "https://images.unsplash.com/photo-1571877227200-a0d6a6c6adef?w=800&q=80&fit=crop",
            averageScore = 9.6, totalRatings = 89,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1708700000000
        ),
        Plate(
            id = "plate_014",
            name = "Pad Thai de Gambas",
            description = "Fideos de arroz salteados con gambas, cacahuetes y lima fresca",
            category = PlateCategory.OTHER,
            restaurantName = "Rosa's Thai Cafe",
            restaurantAddress = "48 Dean Street",
            city = "Londres", country = "Reino Unido",
            imageUrl = "https://images.unsplash.com/photo-1559314809-0d155014e29e?w=800&q=80&fit=crop",
            averageScore = 8.7, totalRatings = 33,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1708600000000
        ),
        Plate(
            id = "plate_015",
            name = "Gambas al Ajillo",
            description = "Gambas frescas en aceite de oliva con ajo, guindilla y perejil",
            category = PlateCategory.SEAFOOD,
            restaurantName = "Casa Benigna",
            restaurantAddress = "Calle Benigno Soto, 9",
            city = "Madrid", country = "España",
            imageUrl = "https://images.unsplash.com/photo-1565680018434-b513d6a4c1e4?w=800&q=80&fit=crop",
            averageScore = 9.1, totalRatings = 61,
            addedByUserId = "seed_user", addedByUserName = "FoodRanker Team", createdAt = 1708500000000
        )
    )
}
