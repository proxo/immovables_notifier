# Flag marking link as visited

# --- !Ups

alter table estates add column viewed boolean default false ;

# --- !Downs
alter table estates drop column if exists viewed CASCADE;
