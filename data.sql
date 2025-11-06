CREATE DATABASE securefile_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'your_username'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON securefile_db.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;