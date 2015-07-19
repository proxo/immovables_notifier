
create unique index  ESTATES_UQ_GUID ON estates (guid);


CREATE TABLE IF NOT EXISTS parse_ledger (
  id serial primary key,
  created_at timestamp default current_timestamp,
  ended_at timestamp default current_timestamp,
  new_items integer not null default 0,
  old_items integer not null default 0,
  failed integer not null default 0
);
