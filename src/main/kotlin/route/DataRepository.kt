package com.example.route


import com.example.rout.Sneaker
import com.example.rout.User



object DataRepository {
    val userList = mutableListOf<User>()
    val sneakerList = mutableListOf<Sneaker>()


    init {
        userList.add(
            User(
                userId = 1,
                userName = "Ivan",
                email = "123",
                password = "123",
                favoriteSneakerIds = mutableListOf()
            )
        )
    }


    init {
        sneakerList.addAll(
            listOf(
                Sneaker(
                    id = 1,
                    name = "Nike Air Max",
                    description = "Classic sneakers",
                    price = 732.0,
                    imageUrl = "mainsneakers",
                    category = "Popular",
                    isPopular = true,
                    isFavorite = false,
                    quantity = 1
                ),
                Sneaker(
                    id = 2,
                    name = "Adidas Ultraboost",
                    description = "Running shoes",
                    price = 850.0,
                    imageUrl = "mainsneakers",
                    category = "Popular",
                    isFavorite = false,
                    quantity = 1
                ),
                Sneaker(
                  id = 3,
                  name = "Abibas Crossovok",
                  description = "mega chill",
                  price = 999.0,
                  imageUrl = "mainsneakers",
                  category = "Outdoor",
                  isPopular = true,
                  isFavorite = false,
                  quantity = 1


                )
            )
        )
    }
}