
create table if not exists PARSE_SETS (
  id serial primary key,
  name varchar(100) not null,
  description varchar(255),
  active boolean,
  created_at timestamp default current_timestamp
);

DROP TYPE SITE_NAME;
CREATE TYPE SITE_NAME AS ENUM ('otodom', 'gratka', 'domiporta','domy');

create table if not exists PARSE_SITES (
  id serial primary key,
  name SITE_NAME not null,
  url TEXT,
  set_name varchar(100) not null,
  order_no int,
  created_at timestamp default current_timestamp
);

drop table parse_ledger;

CREATE TABLE IF NOT EXISTS parse_ledger (
  id serial primary key,
  new_items integer not null default 0,
  old_items integer not null default 0,
  failed_items integer not null default 0,
  created_at timestamp default current_timestamp,
  ended_at timestamp default current_timestamp
);

alter table estates drop column if exists set_name;
alter table estates add column set_name varchar (100);

truncate table parse_sets;
truncate table parse_sites;

