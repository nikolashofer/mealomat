-- The repeating week template. No foreign keys: sync pulls table by table in pages, so a child
-- row legitimately arrives before its parent.
--
-- There is no plan_day table: the plan is exactly seven days, so the day is a value on the meal.

create table plan_meal (
  id         uuid        primary key,
  user_id    uuid        not null,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  weekday     smallint   not null,   -- ISO day number: 1 = Monday ... 7 = Sunday
  name        text       not null,
  position    integer    not null
);

create index plan_meal_user_id on plan_meal(user_id);
create index plan_meal_weekday on plan_meal(weekday);

alter table plan_meal enable row level security;

create policy plan_meal_owner on plan_meal
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

create table plan_component (
  id           uuid        primary key,
  user_id      uuid        not null,
  plan_meal_id uuid        not null,
  updated_at   timestamptz not null default now(),
  deleted_at   timestamptz,
  name         text        not null,
  position     integer   not null,
  prep_mode    text      not null      -- PREP | FRESH
);

create index plan_component_user_id on plan_component(user_id);
create index plan_component_meal on plan_component(plan_meal_id);

alter table plan_component enable row level security;

create policy plan_component_owner on plan_component
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

create table plan_item (
  id                uuid        primary key,
  user_id           uuid        not null,
  plan_meal_id      uuid        not null,
  plan_component_id uuid,                       -- null = flat, directly on the meal
  ingredient_id     uuid        not null,
  updated_at        timestamptz not null default now(),
  deleted_at        timestamptz,
  amount            double precision not null,
  position          integer not null,
  prep_mode         text                     -- null = inherit from the component
);

create index plan_item_user_id on plan_item(user_id);
create index plan_item_meal on plan_item(plan_meal_id);
create index plan_item_component on plan_item(plan_component_id);

alter table plan_item enable row level security;

create policy plan_item_owner on plan_item
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );
