

# --- !Ups

alter table estates
    alter column area TYPE numeric(16,2),
    alter column price TYPE numeric(16,2);


# --- !Downs

