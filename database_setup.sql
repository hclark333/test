-- Create the database
create database if not exists csx370_mb_platform;

-- Use the created database
use csx370_mb_platform;

-- Create the user table
create table if not exists user (
    userId int auto_increment,
    username varchar(255) not null,
    password varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    primary key (userId),
    unique (username),
    constraint userName_min_length check (char_length(trim(username)) >= 2),
    constraint firstName_min_length check (char_length(trim(firstName)) >= 2),
    constraint lastName_min_length check (char_length(trim(lastName)) >= 2)
    );

-- Create post table
create table if not exists post (
    postId int auto_increment,
    userId int not null,
    content text not null,
    postDate datetime default current_timestamp,
    heartsCount int default 0,
    commentsCount int default 0,
    primary key (postId),
    foreign key (userId) references user(userId) on delete cascade,
    constraint content_not_empty check (char_length(trim(content)) > 0),
    constraint heartsCount_non_negative check (heartsCount >= 0),
    constraint commentsCount_non_negative check (commentsCount >= 0),
    index idx_user_date (userId, postDate desc),
    index idx_date (postDate desc)
    );

-- Create expanded post
create table if not exists bookmark (
    postId int not null,
    userId int not null,
    bookmarkDate datetime default current_timestamp,
    primary key (postId, userId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (userId) references user(userId) on delete cascade,
    index idx_user_date (userId, bookmarkDate desc)
    );

create table if not exists heart (
    postId int not null,
    userId int not null,
    heartDate datetime default current_timestamp,
    primary key (postId, userId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (userId) references user(userId) on delete cascade,
    index idx_post (postId)
    );

-- Create comment table
create table if not exists comment (
    commentId int auto_increment,
    postId int not null,
    userId int not null,
    content text not null,
    commentDate datetime default current_timestamp,
    primary key (commentId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (userId) references user(userId) on delete cascade,
    constraint comment_not_empty check (char_length(trim(content)) > 0),
    index idx_post_date (postId, commentDate asc)
    );

-- Create hashtag table
create table if not exists hashtag (
    hashtagId int auto_increment,
    tagName varchar(100) not null,
    primary key (hashtagId),
    unique (tagName),
    constraint tag_format check (tagName regexp '^#[a-zA-Z0-9_]+$')
    );

-- Create post_hashtag junction table (many-to-many)
create table if not exists post_hashtag (
    postId int,
    hashtagId int,
    primary key (postId, hashtagId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (hashtagId) references hashtag(hashtagId) on delete cascade
    );

-- Create follows table (many-to-many self-referencing)
create table if not exists follows (
    followerId int,
    followeeId int,
    followDate datetime default current_timestamp,
    primary key (followerId, followeeId),
    foreign key (followerId) references user(userId) on delete cascade,
    foreign key (followeeId) references user(userId) on delete cascade,
    constraint no_self_follow check (followerId != followeeId),
    index idx_follower (followerId),
    index idx_followee (followeeId)
    );

-- If you want to initialize database without docker cp, just move this file
-- into the resources folder and uncomment the sql-init lines in application.properties.

-- Create hashtag table
create table if not exists hashtag (
    hashtagId int auto_increment,
    tagName varchar(100) not null,
    primary key (hashtagId),
    unique (tagName),
    constraint tag_format check (tagName regexp '^#[a-zA-Z0-9_]+$')
    );

-- Create post_hashtag junction table (many-to-many)
create table if not exists post_hashtag (
    postId int,
    hashtagId int,
    primary key (postId, hashtagId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (hashtagId) references hashtag(hashtagId) on delete cascade
    );

-- Create follows table (many-to-many self-referencing)
create table if not exists follows (
    followerId int,
    followeeId int,
    followDate datetime default current_timestamp,
    primary key (followerId, followeeId),
    foreign key (followerId) references user(userId) on delete cascade,
    foreign key (followeeId) references user(userId) on delete cascade,
    constraint no_self_follow check (followerId != followeeId),
    index idx_follower (followerId),
    index idx_followee (followeeId)
    );

-- Create heart/like table (many-to-many)
create table if not exists heart (
    userId int,
    postId int,
    heartDate datetime default current_timestamp,
    primary key (userId, postId),
    foreign key (userId) references user(userId) on delete cascade,
    foreign key (postId) references post(postId) on delete cascade,
    index idx_post (postId)
    );

-- Create bookmark table (many-to-many)
create table if not exists bookmark (
    userId int,
    postId int,
    bookmarkDate datetime default current_timestamp,
    primary key (userId, postId),
    foreign key (userId) references user(userId) on delete cascade,
    foreign key (postId) references post(postId) on delete cascade,
    index idx_user_date (userId, bookmarkDate desc)
    );