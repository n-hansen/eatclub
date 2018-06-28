CREATE TABLE IF NOT EXISTS restaurants
(id IDENTITY PRIMARY KEY,
name VARCHAR(32));
--;;
CREATE TABLE IF NOT EXISTS categories
(id IDENTITY PRIMARY KEY,
name VARCHAR(32));
--;;
CREATE TABLE IF NOT EXISTS items
(id INT PRIMARY KEY,
name VARCHAR(45),
calories VARCHAR(4),
fat VARCHAR(4),
protein VARCHAR(4),
carbs VARCHAR(4),
price DECIMAL(4,2),
restaurant BIGINT,
category BIGINT,
FOREIGN KEY (restaurant) REFERENCES restaurants(id),
FOREIGN KEY (category) REFERENCES categories(id));
--;;
CREATE TABLE IF NOT EXISTS menu_snapshots
(id IDENTITY PRIMARY KEY,
snapshot_time TIMESTAMP,
menu_date DATE);
--;;
CREATE TABLE item_listing
(id IDENTITY PRIMARY KEY,
menu_snapshot BIGINT,
item INT,
quantity INT,
hidden BOOLEAN,
average_rating DOUBLE,
review_count INT,
FOREIGN KEY (menu_snapshot) REFERENCES menu_snapshots(id),
FOREIGN KEY (item) REFERENCES items(id));
