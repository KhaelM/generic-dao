
CREATE TABLE REALISATEUR(
	id integer PRIMARY KEY,
	nom varchar(30)
);
CREATE SEQUENCE realisateur_seq start with 1;
CREATE TABLE FILM(
	id integer PRIMARY KEY,
	nom varchar(30),
	id_realisateur integer
);
ALTER TABLE film ADD CONSTRAINT film_fk FOREIGN KEY(id_realisateur) REFERENCES REALISATEUR(id);
CREATE SEQUENCE film_seq start with 1;
CREATE TABLE "ActeurDansFilm"(
	id integer PRIMARY KEY,
	id_film integer,
	nom varchar(30),
	date_naissance date,
	date_mort date,
	CONSTRAINT acteur_fk FOREIGN KEY(id_film) REFERENCES FILM(id)
);
CREATE SEQUENCE acteur_seq start with 1;

CREATE OR REPLACE TRIGGER realisateur_trig
BEFORE INSERT ON REALISATEUR 
FOR EACH ROW

BEGIN
	SELECT realisateur_seq.nextval INTO :new.id FROM DUAL;
END;
/
CREATE OR REPLACE TRIGGER film_trig
BEFORE INSERT ON FILM 
FOR EACH ROW

BEGIN
	SELECT film_seq.nextval INTO :new.id FROM DUAL;
END;
/

CREATE OR REPLACE TRIGGER acteur_trig
BEFORE INSERT ON "ActeurDansFilm"
FOR EACH ROW

BEGIN
	SELECT acteur_seq.nextval INTO :new.id FROM DUAL;
END;
/



INSERT INTO REALISATEUR(nom) values('Spielberg');
INSERT INTO REALISATEUR(nom) values('Coppola');
INSERT INTO REALISATEUR(nom) values('Scorcese');

INSERT INTO FILM(nom,id_realisateur) values ('Ready Player One',1);
INSERT INTO FILM(nom,id_realisateur) values ('GodFather',2);
INSERT INTO FILM(nom,id_realisateur) values ('Son Film',3);

INSERT INTO  "ActeurDansFilm"(id_film,nom) values (1,'Brad Pitt'); 
INSERT INTO "ActeurDansFilm"(id_film,nom) values (1,'Bred Pitt'); 

INSERT INTO  "ActeurDansFilm"(id_film,nom) values (2,'Brud Pitt'); 
INSERT INTO  "ActeurDansFilm"(id_film,nom) values (2,'Brid Pitt'); 

INSERT INTO "ActeurDansFilm"(id_film,nom) values (3,'Brad Patt'); 
INSERT INTO "ActeurDansFilm"(id_film,nom) values (3,'Brad Pott'); 