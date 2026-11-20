create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(100) not null,
    role          varchar(20)  not null,
    enabled       boolean      not null default true,
    created_at    timestamptz  not null default now()
);

create table refresh_tokens (
    id         bigserial primary key,
    token      varchar(100) not null unique,
    expires_at timestamptz  not null,
    revoked    boolean      not null default false,
    user_id    bigint       not null references users (id)
);
create index idx_refresh_tokens_user on refresh_tokens (user_id);

create table venues (
    id             bigserial primary key,
    name           varchar(150) not null,
    address_line   varchar(255) not null,
    city           varchar(100) not null,
    timezone       varchar(50)  not null,
    total_capacity integer      not null
);
create index idx_venues_city on venues (city);

create table sections (
    id            bigserial primary key,
    name          varchar(100) not null,
    row_count     integer      not null,
    seats_per_row integer      not null,
    price_tier    varchar(20)  not null,
    venue_id      bigint       not null references venues (id)
);
create index idx_sections_venue on sections (venue_id);

create table seats (
    id          bigserial primary key,
    row_label   varchar(5) not null,
    seat_number integer    not null,
    accessible  boolean    not null default false,
    section_id  bigint     not null references sections (id),
    constraint uq_seat_position unique (section_id, row_label, seat_number)
);

create table events (
    id             bigserial primary key,
    title          varchar(200)  not null,
    description    varchar(2000),
    starts_at      timestamptz   not null,
    doors_open_at  timestamptz,
    status         varchar(20)   not null,
    sales_open_at  timestamptz   not null,
    sales_close_at timestamptz   not null,
    created_at     timestamptz   not null default now(),
    venue_id       bigint        not null references venues (id),
    organizer_id   bigint        not null references users (id)
);
create index idx_events_venue on events (venue_id);
create index idx_events_organizer on events (organizer_id);
create index idx_events_status on events (status);
create index idx_events_starts_at on events (starts_at);

create table event_section_prices (
    id         bigserial primary key,
    price      numeric(10, 2) not null,
    currency   varchar(3)     not null,
    event_id   bigint         not null references events (id),
    section_id bigint         not null references sections (id),
    constraint uq_event_section_price unique (event_id, section_id)
);
create index idx_event_section_prices_section on event_section_prices (section_id);

create table seat_holds (
    id             bigserial primary key,
    hold_reference varchar(40) not null unique,
    expires_at     timestamptz not null,
    status         varchar(20) not null,
    created_at     timestamptz not null default now(),
    event_id       bigint      not null references events (id),
    user_id        bigint      not null references users (id)
);
create index idx_seat_holds_event on seat_holds (event_id);
create index idx_seat_holds_user on seat_holds (user_id);
create index idx_seat_holds_status_expires on seat_holds (status, expires_at);

create table seat_hold_seats (
    hold_id bigint not null references seat_holds (id),
    seat_id bigint not null references seats (id),
    primary key (hold_id, seat_id)
);
create index idx_seat_hold_seats_seat on seat_hold_seats (seat_id);

create table bookings (
    id                bigserial primary key,
    booking_reference varchar(40)    not null unique,
    status            varchar(20)    not null,
    subtotal          numeric(10, 2) not null,
    fees              numeric(10, 2) not null,
    total             numeric(10, 2) not null,
    currency          varchar(3)     not null,
    created_at        timestamptz    not null default now(),
    confirmed_at      timestamptz,
    cancelled_at      timestamptz,
    version           bigint         not null default 0,
    event_id          bigint         not null references events (id),
    user_id           bigint         not null references users (id)
);
create index idx_bookings_event on bookings (event_id);
create index idx_bookings_user on bookings (user_id);
create index idx_bookings_status on bookings (status);

create table tickets (
    id            bigserial primary key,
    ticket_code   varchar(40)    not null unique,
    status        varchar(20)    not null,
    issued_at     timestamptz,
    checked_in_at timestamptz,
    price_paid    numeric(10, 2) not null,
    booking_id    bigint         not null references bookings (id),
    seat_id       bigint         not null references seats (id),
    event_id      bigint         not null references events (id),
    constraint uq_ticket_event_seat unique (event_id, seat_id)
);
create index idx_tickets_booking on tickets (booking_id);
create index idx_tickets_seat on tickets (seat_id);

create table payments (
    id             bigserial primary key,
    transaction_id varchar(100),
    method         varchar(20)    not null,
    status         varchar(20)    not null,
    amount         numeric(10, 2) not null,
    currency       varchar(3)     not null,
    failure_code   varchar(50),
    paid_at        timestamptz,
    booking_id     bigint         not null unique references bookings (id)
);

create table outbox_messages (
    id             bigserial primary key,
    aggregate_type varchar(50) not null,
    aggregate_id   varchar(50) not null,
    event_type     varchar(50) not null,
    payload        jsonb       not null,
    created_at     timestamptz not null default now(),
    published_at   timestamptz
);
create index idx_outbox_unpublished on outbox_messages (created_at) where published_at is null;

create table processed_messages (
    id          bigserial primary key,
    message_id  varchar(100) not null unique,
    consumed_at timestamptz  not null default now()
);

create table audit_entries (
    id          bigserial primary key,
    actor_id    bigint,
    action      varchar(50) not null,
    entity_type varchar(50) not null,
    entity_id   varchar(50) not null,
    created_at  timestamptz not null default now()
);
create index idx_audit_entries_entity on audit_entries (entity_type, entity_id);
