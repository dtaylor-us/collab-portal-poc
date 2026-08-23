create table dpp_review (
  review_id uuid primary key,
  dpp_result_id varchar(120) not null,
  transmission_owner_id varchar(120) not null,
  status varchar(40) not null,
  process_instance_key bigint,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table correction_submission (
  correction_id uuid primary key,
  review_id uuid not null references dpp_review(review_id),
  version integer not null,
  comment text not null,
  created_at timestamptz not null,
  unique (review_id, version)
);

create table miso_disposition (
  disposition_id uuid primary key,
  review_id uuid not null references dpp_review(review_id),
  correction_version integer not null,
  decision varchar(40) not null,
  comment text,
  created_at timestamptz not null
);

create table idempotency_command (
  idempotency_key varchar(160) primary key,
  operation varchar(60) not null,
  review_id uuid not null,
  request_hash varchar(64) not null,
  state varchar(30) not null,
  created_at timestamptz not null,
  completed_at timestamptz
);

create index idx_dpp_review_created on dpp_review(created_at desc);
create index idx_correction_review on correction_submission(review_id, version);
create index idx_disposition_review on miso_disposition(review_id, created_at);
