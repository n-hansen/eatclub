-- :name create-restaurant! :insert
INSERT INTO restaurants
(name)
VALUES (:name)

-- :name get-restaurant-id :? :1
SELECT id FROM restaurants WHERE name = :name LIMIT 1

-- :name create-category! :insert
INSERT INTO categories
(name)
VALUES (:name)

-- :name get-category-id :? :1
SELECT id FROM categories WHERE name = :name LIMIT 1

-- :name create-item! :execute
INSERT INTO items
(id,name,calories,fat,protein,carbs,price,restaurant,category)
VALUES (:id,:name,:calories,:fat,:protein,:carbs,:price,:restaurant,:category)

-- :name get-item :? :1
SELECT * FROM items WHERE id = :id

-- :name create-menu-snapshot! :insert
INSERT INTO menu_snapshots
(snapshot_time,menu_date)
VALUES (:snapshot_time,:menu_date)

-- :name create-item-listing! :insert
INSERT INTO item_listing
(menu_snapshot,item,quantity,hidden,average_rating,review_count)
VALUES (:menu_snapshot,:item,:quantity,:hidden,:average_rating,:review_count)
