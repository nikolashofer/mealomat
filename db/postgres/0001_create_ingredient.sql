create table ingredient (
  id              uuid        primary key,
  user_id         uuid        not null,
  updated_at      timestamptz not null default now(),
  deleted_at      timestamptz,
  name            text        not null,
  basis           text        not null,          -- G100 | ML100 | UNIT
  kcal            double precision not null,
  protein_g       double precision not null,
  carbs_g         double precision not null,
  fat_g           double precision not null,
  fiber_g         double precision,
  sugar_g         double precision,
  saturated_fat_g double precision,
  salt_g          double precision,
  pack_size       double precision,
  archived        boolean     not null default false,
  note            text
);

create index ingredient_user_id on ingredient(user_id);

alter table ingredient enable row level security;

create policy ingredient_owner on ingredient
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );
