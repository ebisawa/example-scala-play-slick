# Users schema
 
# --- !Ups
 
CREATE TABLE users (
  id          INT(11) NOT NULL AUTO_INCREMENT,
  name        VARCHAR(255) NOT NULL,
  company_id  INT(11) DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE companies (
  id          INT(11) NOT NULL AUTO_INCREMENT,
  name        VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

 
# --- !Downs
 
DROP TABLE companies;
DROP TABLE users;
