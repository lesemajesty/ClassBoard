-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: classboard
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `classboard`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `classboard` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `classboard`;

--
-- Table structure for table `announcement`
--

DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `category` varchar(32) DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `content` text,
  `pinned` bit(1) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `publisher` varchar(32) DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `event_date` date DEFAULT NULL,
  `expire_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `canteen_menu`
--

DROP TABLE IF EXISTS `canteen_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `canteen_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` int NOT NULL,
  `dishes` text,
  `meal` enum('BREAKFAST','DINNER','LUNCH') NOT NULL,
  `note` varchar(128) DEFAULT NULL,
  `price` varchar(32) DEFAULT NULL,
  `week_start` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe7hitnurghw3qcvd7lsw6b37q` (`week_start`,`day_of_week`,`meal`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `check_in_record`
--

DROP TABLE IF EXISTS `check_in_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `check_in_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `checked_at` datetime(6) NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `late` bit(1) NOT NULL,
  `student_id` bigint DEFAULT NULL,
  `student_name` varchar(32) NOT NULL,
  `type` enum('IN','OUT') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `class_info`
--

DROP TABLE IF EXISTS `class_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `classroom` varchar(64) DEFAULT NULL,
  `grade` varchar(32) DEFAULT NULL,
  `head_teacher` varchar(32) DEFAULT NULL,
  `motto` varchar(128) DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `photo_url` varchar(255) DEFAULT NULL,
  `student_count` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `countdown_event`
--

DROP TABLE IF EXISTS `countdown_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `countdown_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_date` date NOT NULL,
  `name` varchar(64) NOT NULL,
  `note` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `day_of_week` int NOT NULL,
  `end_time` varchar(8) DEFAULT NULL,
  `period` int NOT NULL,
  `start_time` varchar(8) DEFAULT NULL,
  `subject` varchar(32) DEFAULT NULL,
  `teacher` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `duty_roster`
--

DROP TABLE IF EXISTS `duty_roster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `duty_roster` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `day_of_week` int NOT NULL,
  `students` varchar(128) DEFAULT NULL,
  `task` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKoa544gwh4g2phdx1mox6hvynt` (`class_id`,`day_of_week`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `homework`
--

DROP TABLE IF EXISTS `homework`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `content` text,
  `created_at` datetime(6) NOT NULL,
  `due_date` date NOT NULL,
  `subject` varchar(32) NOT NULL,
  `teacher` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `leave_record`
--

DROP TABLE IF EXISTS `leave_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved` bit(1) NOT NULL,
  `class_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `start_date` date NOT NULL,
  `student_id` bigint DEFAULT NULL,
  `student_name` varchar(32) NOT NULL,
  `type` varchar(16) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `news_item`
--

DROP TABLE IF EXISTS `news_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `category` varchar(32) DEFAULT NULL,
  `featured` bit(1) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `source` varchar(64) DEFAULT NULL,
  `summary` text,
  `title` varchar(128) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `external_key` varchar(128) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6ydcrtar6s24jrubw5wkt75ap` (`external_key`)
) ENGINE=InnoDB AUTO_INCREMENT=161 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `song_request`
--

DROP TABLE IF EXISTS `song_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `song_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `artist` varchar(64) DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `played_at` datetime(6) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `requester` varchar(32) DEFAULT NULL,
  `song` varchar(64) NOT NULL,
  `status` enum('APPROVED','PENDING','PLAYED','REJECTED') NOT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reject_read_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `birthday` date DEFAULT NULL,
  `card_no` varchar(32) NOT NULL,
  `class_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjbf5xqohc9kv23srlqabvqfub` (`card_no`)
) ENGINE=InnoDB AUTO_INCREMENT=287 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_name` varchar(32) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `password` varchar(128) NOT NULL,
  `role` varchar(32) NOT NULL,
  `username` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK51bvuyvihefoh4kp5syh2jpi4` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `terminal_device`
--

DROP TABLE IF EXISTS `terminal_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terminal_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint DEFAULT NULL,
  `code` varchar(64) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `last_seen_at` datetime(6) DEFAULT NULL,
  `location` varchar(64) DEFAULT NULL,
  `name` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKab15forfra3a6nl9hrh45bb74` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `weather_forecast`
--

DROP TABLE IF EXISTS `weather_forecast`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weather_forecast` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(32) NOT NULL,
  `weather_condition` varchar(32) NOT NULL,
  `forecast_date` date NOT NULL,
  `temp_high` int DEFAULT NULL,
  `temp_low` int DEFAULT NULL,
  `tip` varchar(128) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtanoc4qag5699bvbe4ilq4iu3` (`forecast_date`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-07 17:44:54
