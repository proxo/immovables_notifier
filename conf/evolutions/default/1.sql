

CREATE TABLE IF NOT EXISTS estates (
  id serial CONSTRAINT estates_pk PRIMARY KEY ,
  site varchar(100) default 'gratka',
  guid varchar(120) NOT NULL,
  title varchar(100),
  pub_date date ,
  area  numeric(8,2),
  price numeric(8,2),
  rooms integer,
  stock integer,
  total_stocks integer,
  built_year integer,
  created_at timestamp default current_timestamp,
  modified_at timestamp default current_timestamp
);


CREATE TABLE  IF NOT EXISTS estate_rel (
  estate_id_1 integer not null REFERENCES estates (id),
  estate_id_2 integer not null REFERENCES estates (id),
  similarity numeric(12,6) not null default 0.0,
  created_at timestamp default current_timestamp
);
