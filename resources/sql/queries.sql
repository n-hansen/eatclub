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
(snapshot_time,menu_date,full_response)
VALUES (:snapshot_time,:menu_date,:full_response)

-- :name create-item-listing! :insert
INSERT INTO item_listing
(menu_snapshot,item,quantity,hidden,average_rating,review_count)
VALUES (:menu_snapshot,:item,:quantity,:hidden,:average_rating,:review_count)

-- :name get-menu-items :? :*
SELECT
  DISTINCT i.id AS item_id
FROM
  menu_snapshots ms
  INNER JOIN item_listing il ON il.menu_snapshot = ms.id
  INNER JOIN items i ON i.id = il.item
WHERE
  ms.menu_date = :menu_date

-- :name get-snapshots* :? :*
SELECT
  snapshot_time AS timestamp,
  quantity,
  hidden,
  average_rating,
  review_count
FROM
  menu_snapshots ms
  INNER JOIN item_listing il ON il.menu_snapshot = ms.id
WHERE
  ms.menu_date = :menu_date
  AND il.item = :item
  AND snapshot_time >= :window_start
  AND snapshot_time <= :window_end
ORDER BY
  snapshot_time ASC
