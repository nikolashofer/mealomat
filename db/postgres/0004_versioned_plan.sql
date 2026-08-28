-- Versioned plan and the sparse day overlay. Replaces the week_* snapshot tables: a date resolves to
-- the plan version that owned it, so history is exact without copying a week.

create table plan (
  id         uuid        primary key,
  user_id    uuid        not null,
  updated_at           timestamptz not null default now(),
  deleted_at           timestamptz,
  active_from_date     date        not null,
  active_from_position integer     not null
);

create index plan_user_id on plan(user_id);

-- user_id is part of the key: this table holds every user's rows, and RLS filters what a query sees
-- without scoping a unique constraint.
create unique index plan_activation on plan(user_id, active_from_date, active_from_position)
  where deleted_at is null;

alter table plan enable row level security;

create policy plan_owner on plan
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

-- day_item is sparse and holds no amount: the amount lives in the immutable version that owns the
-- date, so the logbook and the pantry ledger cannot drift apart.
create table day_item (
  id         uuid        primary key,
  user_id    uuid        not null,
  plan_item_id uuid        not null,
  updated_at   timestamptz not null default now(),
  deleted_at   timestamptz,
  committed_at timestamptz,   -- a shopping or prep session covered this line
  prepped_at   timestamptz,
  ticked_at    timestamptz,
  date         date        not null,
  excluded     boolean     not null default false
);

create index day_item_user_id on day_item(user_id);

-- Partial, and its leading date also serves listForDate, so no separate index on date is needed.
create unique index day_item_line on day_item(date, plan_item_id) where deleted_at is null;

alter table day_item enable row level security;

create policy day_item_owner on day_item
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

-- The plan children gain plan_id, and plan_component gains a lineage that survives versioning so a
-- saved prep step ordering (prep_step_override.target_key) still resolves after a revision.
alter table plan_meal      add column plan_id uuid not null;
alter table plan_component add column plan_id uuid not null,
                           add column lineage_id uuid not null;
alter table plan_item      add column plan_id uuid not null;

create index plan_meal_plan on plan_meal(plan_id);
create index plan_component_plan on plan_component(plan_id);
create index plan_component_lineage on plan_component(lineage_id);
create index plan_item_plan on plan_item(plan_id);
