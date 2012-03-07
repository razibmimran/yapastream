-- phpMyAdmin SQL Dump
-- version 3.3.7deb6
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Nov 10, 2011 at 03:03 PM
-- Server version: 5.1.49
-- PHP Version: 5.3.3-7+squeeze3

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `yapastream`
--
-- --------------------------------------------------------

--
-- Table structure for table `passwordConfirmation`
--

CREATE TABLE IF NOT EXISTS `passwordConfirmation` (
  `id` int(11) NOT NULL,
  `username` varchar(32) NOT NULL,
  `confirmation_code` varchar(32) NOT NULL,
  `create_date` bigint(20) NOT NULL,
  KEY `username` (`username`),
  KEY `confirmation_code` (`confirmation_code`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `passwordConfirmation`
--


-- --------------------------------------------------------

--
-- Table structure for table `profile`
--

CREATE TABLE IF NOT EXISTS `profile` (
  `username` varchar(32) NOT NULL,
  `information` varchar(2048) NOT NULL,
  `location` varchar(64) NOT NULL,
  UNIQUE KEY `username` (`username`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `profile`
--


-- --------------------------------------------------------

--
-- Table structure for table `settings`
--

CREATE TABLE IF NOT EXISTS `settings` (
  `username` varchar(32) NOT NULL,
  `privacy` int(11) NOT NULL COMMENT '0=show on home page, 1=show in profile, 2=require login',
  `resolution` int(11) DEFAULT NULL COMMENT '1=high,2=low',
  PRIMARY KEY (`username`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Dumping data for table `settings`
--


-- --------------------------------------------------------

--
-- Table structure for table `streams`
--

CREATE TABLE IF NOT EXISTS `streams` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(32) NOT NULL,
  `create_date` bigint(15) NOT NULL,
  `active` int(1) NOT NULL,
  `session_id` int(32) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `active` (`active`),
  KEY `username` (`username`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Dumping data for table `streams`
--


-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(32) NOT NULL,
  `password` varchar(1023) NOT NULL,
  `email` varchar(64) NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`username`),
  UNIQUE KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Dumping data for table `users`
--

