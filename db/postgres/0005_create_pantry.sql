-- One continuous stock, as an append-only log of signed deltas. Rows are never updated or deleted,
-- so two devices adding rows merge by union and the sum is correct either way.
--
-- pantry_stock has no mirror here: it is a local fold of this table, rebuilt after every pull.

create table pantry_ledger (
  id            uuid        primary key,
  user_id       uuid        not null,
  ingredient_id uuid        not null,
  updated_at    timestamptz not null default now(),
  occurred_at   timestamptz not null,   -- when the movement happened, not when the row was written
  delta         double precision not null,   -- signed, in the ingredient's basis unit
  reason        text not null,   -- BUY | PREP | COOK | CONSUME | SPOILAGE | ADJUST
  source_kind   text not null,   -- SHOPPING_LINE | PREP_STEP | DAY_ITEM | MANUAL
  source_id     uuid,            -- the causing row, null when source_kind is MANUAL
  note          text
);

create index pantry_ledger_user_id on pantry_ledger(user_id);
create index pantry_ledger_ingredient on pantry_ledger(ingredient_id, occurred_at);

alter table pantry_ledger enable row level security;

-- Read and append only. RLS denies whatever no policy permits, so the absence of an update and a
-- delete policy is what enforces append-only in the database rather than in the client (§8).
create policy pantry_ledger_read on pantry_ledger
  for select
  using ( user_id = auth.uid() );

create policy pantry_ledger_append on pantry_ledger
  for insert
  with check ( user_id = auth.uid() );
