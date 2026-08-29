-- A trip shops for one prep block's coverage window, frozen onto it because blocks are editable.
-- Lines are sparse: a row exists because something was bought or skipped, carrying the numbers the
-- wizard showed at that moment. Everything untouched is derived, so it cannot go stale.

create table shopping_trip (
  id            uuid        primary key,
  user_id       uuid        not null,
  prep_block_id uuid        not null,
  updated_at    timestamptz not null default now(),
  deleted_at    timestamptz,
  started_at    timestamptz,
  completed_at  timestamptz,
  planned_date         date not null,
  covers_from_date     date    not null,   -- the window this trip shopped for
  covers_from_position integer not null,
  covers_to_date       date    not null,   -- exclusive: the next block's boundary
  covers_to_position   integer not null,
  status           text not null    -- IN_PROGRESS | DONE | ABANDONED
);

create index shopping_trip_user_id on shopping_trip(user_id);
create index shopping_trip_block on shopping_trip(prep_block_id);

-- user_id is what scopes this: the constraint is enforced against every row in the table, and RLS
-- filters visibility rather than uniqueness.
create unique index shopping_trip_open on shopping_trip(user_id)
  where status = 'IN_PROGRESS' and deleted_at is null;

alter table shopping_trip enable row level security;

create policy shopping_trip_owner on shopping_trip
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

create table shopping_line (
  id               uuid        primary key,
  user_id          uuid        not null,
  shopping_trip_id uuid        not null,
  ingredient_id    uuid        not null,
  updated_at       timestamptz not null default now(),
  deleted_at       timestamptz,
  skipped_at       timestamptz,                 -- decided against; the need carries to the next trip
  needed_amount    double precision not null,   -- planned across the window when the action was taken
  have_amount      double precision not null,   -- pantry stock then, less what this trip had bought
  suggested_amount double precision not null,   -- max(0, needed - have), rounded to whole packs
  bought_amount    double precision             -- entering this is what makes the line bought
);

create index shopping_line_user_id on shopping_line(user_id);
create index shopping_line_trip on shopping_line(shopping_trip_id);

-- shopping_trip_id is a client-generated UUIDv7 that only appears in one user's rows, so it scopes
-- this index on its own.
create unique index shopping_line_ingredient on shopping_line(shopping_trip_id, ingredient_id)
  where deleted_at is null;

alter table shopping_line enable row level security;

create policy shopping_line_owner on shopping_line
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );

-- Commitment moved to the trip's frozen window (see 0006's shopping_trip), so a shopping or prep
-- session no longer stamps every covered line.
alter table day_item drop column committed_at;
