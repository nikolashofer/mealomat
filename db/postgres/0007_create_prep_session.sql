-- A session preps for one block's coverage window, frozen onto it because blocks are editable.
-- Its steps are derived, not stored: checking one off marks the day_item lines it covers and appends
-- one PREP ledger row per line, which is the whole record.

create table prep_session (
  id            uuid        primary key,
  user_id       uuid        not null,
  prep_block_id uuid        not null,
  updated_at    timestamptz not null default now(),
  deleted_at    timestamptz,
  started_at    timestamptz,
  completed_at  timestamptz,
  planned_date         date not null,
  covers_from_date     date    not null,   -- the window this session prepped for
  covers_from_position integer not null,
  covers_to_date       date    not null,   -- exclusive: the next block's boundary
  covers_to_position   integer not null,
  status        text not null              -- IN_PROGRESS | DONE | ABANDONED
);

create index prep_session_user_id on prep_session(user_id);
create index prep_session_block on prep_session(prep_block_id);

-- user_id is what scopes this: the constraint is enforced against every row in the table, and RLS
-- filters visibility rather than uniqueness.
create unique index prep_session_open on prep_session(user_id)
  where status = 'IN_PROGRESS' and deleted_at is null;

alter table prep_session enable row level security;

create policy prep_session_owner on prep_session
  for all
  using      ( user_id = auth.uid() )
  with check ( user_id = auth.uid() );
