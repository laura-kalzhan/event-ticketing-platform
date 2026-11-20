insert into users (id, email, password_hash, display_name, role) values
    (1, 'admin@kimep.kz',          '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Site Admin',      'ADMIN'),
    (2, 'organizer.one@kimep.kz',  '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Aigerim Events',  'ORGANIZER'),
    (3, 'organizer.two@kimep.kz',  '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Steppe Concerts', 'ORGANIZER'),
    (4, 'customer1@kimep.kz',      '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Dana Customer',   'CUSTOMER'),
    (5, 'customer2@kimep.kz',      '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Arman Customer',  'CUSTOMER'),
    (6, 'customer3@kimep.kz',      '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Madina Customer', 'CUSTOMER'),
    (7, 'customer4@kimep.kz',      '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Timur Customer',  'CUSTOMER'),
    (8, 'customer5@kimep.kz',      '$2a$10$AltlqjMjHn.EkPqLboKkI.JhETtYTvuGUxwqVQhlgZdfbPEoagsEi', 'Aliya Customer',  'CUSTOMER');

insert into venues (id, name, address_line, city, timezone, total_capacity) values
    (1, 'Almaty Arena',       'Momyshuly St 44',  'Almaty', 'Asia/Almaty', 1500),
    (2, 'KIMEP Concert Hall', 'Abai Ave 4',       'Almaty', 'Asia/Almaty', 370),
    (3, 'Astana Jazz Club',   'Kunayev St 12',    'Astana', 'Asia/Almaty', 116);

insert into sections (id, name, row_count, seats_per_row, price_tier, venue_id) values
    (1, 'Floor',         20, 30, 'PREMIUM',  1),
    (2, 'North Tribune', 15, 30, 'STANDARD', 1),
    (3, 'South Tribune', 15, 30, 'STANDARD', 1),
    (4, 'Stalls',        10, 25, 'PREMIUM',  2),
    (5, 'Balcony',        6, 20, 'STANDARD', 2),
    (6, 'Main Room',      8, 12, 'STANDARD', 3),
    (7, 'VIP',            2, 10, 'PREMIUM',  3);

insert into seats (row_label, seat_number, accessible, section_id)
select chr(64 + r), n, (r = 1 and n <= 2), s.id
from sections s,
     generate_series(1, s.row_count) r,
     generate_series(1, s.seats_per_row) n
order by s.id, r, n;

insert into events (id, title, description, starts_at, doors_open_at, status, sales_open_at, sales_close_at, venue_id, organizer_id) values
    (1,  'Winter Gala',            'End of year orchestra gala',       now() - interval '60 days',  now() - interval '60 days' - interval '1 hour',  'COMPLETED', now() - interval '120 days', now() - interval '61 days', 2, 2),
    (2,  'Jazz Night Retro',       'Classic jazz standards',           now() - interval '30 days',  now() - interval '30 days' - interval '1 hour',  'COMPLETED', now() - interval '90 days',  now() - interval '31 days', 3, 2),
    (3,  'Tech Conference 2026',   'Talks about software and AI',      now() - interval '10 days',  now() - interval '10 days' - interval '1 hour',  'COMPLETED', now() - interval '60 days',  now() - interval '11 days', 1, 3),
    (4,  'Spring Rock Fest',       'Three bands, one night',           now() + interval '30 days',  now() + interval '30 days' - interval '1 hour',  'ON_SALE',   now() - interval '5 days',   now() + interval '29 days', 1, 2),
    (5,  'Classical Evening',      'Mozart and Tchaikovsky',           now() + interval '20 days',  now() + interval '20 days' - interval '1 hour',  'ON_SALE',   now() - interval '5 days',   now() + interval '19 days', 2, 2),
    (6,  'Startup Summit',         'Founders and investors meetup',    now() + interval '45 days',  now() + interval '45 days' - interval '1 hour',  'ON_SALE',   now() - interval '2 days',   now() + interval '44 days', 2, 3),
    (7,  'Kazakh Folk Festival',   'Dombra and traditional music',     now() + interval '60 days',  now() + interval '60 days' - interval '1 hour',  'ON_SALE',   now() - interval '1 days',   now() + interval '59 days', 1, 3),
    (8,  'Stand-up Comedy Night',  'Local comedians',                  now() + interval '15 days',  now() + interval '15 days' - interval '1 hour',  'ON_SALE',   now() - interval '3 days',   now() + interval '14 days', 3, 3),
    (9,  'Jazz Legends',           'Sold out in one day',              now() + interval '10 days',  now() + interval '10 days' - interval '1 hour',  'SOLD_OUT',  now() - interval '20 days',  now() + interval '9 days',  3, 2),
    (10, 'Summer Open Air',        'Still being planned',              now() + interval '120 days', now() + interval '120 days' - interval '1 hour', 'DRAFT',     now() + interval '30 days',  now() + interval '119 days', 1, 3);

insert into event_section_prices (price, currency, event_id, section_id)
select case when s.price_tier = 'PREMIUM' then 25000 else 12000 end, 'KZT', e.id, s.id
from events e
join sections s on s.venue_id = e.venue_id;

insert into bookings (id, booking_reference, status, subtotal, fees, total, currency, created_at, confirmed_at, cancelled_at, event_id, user_id) values
    (1,  'B-SEED0001', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '80 days', now() - interval '80 days', null, 1, 4),
    (2,  'B-SEED0002', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '79 days', now() - interval '79 days', null, 1, 5),
    (3,  'B-SEED0003', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '78 days', now() - interval '78 days', null, 1, 6),
    (4,  'B-SEED0004', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '50 days', now() - interval '50 days', null, 2, 7),
    (5,  'B-SEED0005', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '49 days', now() - interval '49 days', null, 2, 8),
    (6,  'B-SEED0006', 'CANCELLED',       24000, 1200, 25200, 'KZT', now() - interval '40 days', null, now() - interval '39 days', 3, 4),
    (7,  'B-SEED0007', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '4 days',  now() - interval '4 days',  null, 4, 4),
    (8,  'B-SEED0008', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '4 days',  now() - interval '4 days',  null, 4, 5),
    (9,  'B-SEED0009', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '3 days',  now() - interval '3 days',  null, 4, 6),
    (10, 'B-SEED0010', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '3 days',  now() - interval '3 days',  null, 4, 7),
    (11, 'B-SEED0011', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '2 days',  now() - interval '2 days',  null, 5, 8),
    (12, 'B-SEED0012', 'CANCELLED',       24000, 1200, 25200, 'KZT', now() - interval '2 days',  null, now() - interval '1 days', 4, 8),
    (13, 'B-SEED0013', 'EXPIRED',         50000, 2500, 52500, 'KZT', now() - interval '2 days',  null, now() - interval '2 days', 5, 4),
    (14, 'B-SEED0014', 'CONFIRMED',       0,     0,    0,     'KZT', now() - interval '15 days', now() - interval '15 days', null, 9, 5),
    (15, 'B-SEED0015', 'PENDING_PAYMENT', 0,     0,    0,     'KZT', now(),                      null, null, 4, 6);

with plan (booking_id, section_id, row_label, first_seat, ticket_status) as (values
    (1,  4, 'A', 1,  'CHECKED_IN'),
    (2,  4, 'A', 3,  'CHECKED_IN'),
    (3,  4, 'A', 5,  'CHECKED_IN'),
    (4,  6, 'A', 1,  'CHECKED_IN'),
    (5,  6, 'A', 3,  'CHECKED_IN'),
    (7,  1, 'A', 1,  'ISSUED'),
    (8,  1, 'A', 3,  'ISSUED'),
    (9,  1, 'A', 5,  'ISSUED'),
    (10, 1, 'A', 7,  'ISSUED'),
    (11, 4, 'B', 1,  'ISSUED'),
    (14, 7, 'A', 1,  'ISSUED'),
    (15, 2, 'A', 1,  'RESERVED')
)
insert into tickets (ticket_code, status, issued_at, checked_in_at, price_paid, booking_id, seat_id, event_id)
select 'T-SEED-' || b.id || '-' || st.seat_number,
       p.ticket_status,
       case when p.ticket_status = 'RESERVED' then null else b.created_at end,
       case when p.ticket_status = 'CHECKED_IN' then e.starts_at end,
       esp.price,
       b.id,
       st.id,
       b.event_id
from plan p
join bookings b on b.id = p.booking_id
join events e on e.id = b.event_id
join seats st on st.section_id = p.section_id
             and st.row_label = p.row_label
             and st.seat_number in (p.first_seat, p.first_seat + 1)
join event_section_prices esp on esp.event_id = b.event_id and esp.section_id = p.section_id;

update bookings b
set subtotal = t.amount,
    fees = round(t.amount * 0.05, 2),
    total = t.amount + round(t.amount * 0.05, 2)
from (select booking_id, sum(price_paid) as amount from tickets group by booking_id) t
where t.booking_id = b.id;

insert into payments (transaction_id, method, status, amount, currency, paid_at, booking_id)
select 'SEED-TX-' || b.id, 'CARD', 'SUCCEEDED', b.total, b.currency, b.confirmed_at, b.id
from bookings b
where b.status = 'CONFIRMED';

select setval(pg_get_serial_sequence('users', 'id'), (select max(id) from users));
select setval(pg_get_serial_sequence('venues', 'id'), (select max(id) from venues));
select setval(pg_get_serial_sequence('sections', 'id'), (select max(id) from sections));
select setval(pg_get_serial_sequence('events', 'id'), (select max(id) from events));
select setval(pg_get_serial_sequence('bookings', 'id'), (select max(id) from bookings));
