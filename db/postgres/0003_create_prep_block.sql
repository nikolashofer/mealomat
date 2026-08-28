-- Cycle configuration: which meals a prep block covers, and the order its steps are offered in.

create table prep_block (
  id         uuid        primary key,
  user_id    uuid        not null,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  name             text     not null,
  prep_weekday     smallint not null,   -- ISO day number: 1 = Monday ... 7 = Sunday
  shopping_weekday smallint not null,
  covers_from_weekday  smallint not null,   -- start of this block's coverage window
  covers_from_position integer  not null
);

create index prep_block_user_id on prep_block(user_id);

alter table prep_block enable row level security;

create policy prep_block_owner on prep_block
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

create table prep_step_override (
  id            uuid        primary key,
  user_id       uuid        not null,
  prep_block_id uuid        not null,
  updated_at    timestamptz not null default now(),
  deleted_at    timestamptz,
  target_key    text        not null,   -- "component:<plan_component_id>" | "ingredient:<ingredient_id>"
  position      integer not null
);

create index prep_step_override_user_id on prep_step_override(user_id);
create index prep_step_override_block on prep_step_override(prep_block_id);

alter table prep_step_override enable row level security;

create policy prep_step_override_owner on prep_step_override
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );
