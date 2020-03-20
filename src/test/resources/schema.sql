
create schema IF NOT EXISTS ptdm_o;

CREATE TABLE IF NOT EXISTS ptdm_o.L4_CONSUMER (
  name VARCHAR,
  newKey NUMERIC,
  PRIMARY KEY (newKey),
  UNIQUE (newKey)
);

CREATE TABLE IF NOT EXISTS ptdm_o.L3_CONSUMER (
  destkey3 NUMERIC,
  name VARCHAR,
  businessKey VARCHAR,
  PRIMARY KEY (destKey3),
  UNIQUE (destKey3)
);

CREATE TABLE IF NOT EXISTS ptdm_o.L2_CONSUMER (
  destKey1 NUMERIC,
  destKey2 NUMERIC,
  name VARCHAR,
  businesskey VARCHAR,
  foreign3 numeric not null,
  FOREIGN KEY (foreign3) REFERENCES ptdm_o.L3_CONSUMER (destkey3),
  PRIMARY KEY (destKey1, destKey2),
  UNIQUE (destKey1, destKey2)
);

CREATE TABLE IF NOT EXISTS ptdm_o.L1_CONSUMER (
  name VARCHAR,
  businessKey VARCHAR,
  foreign1 NUMERIC NOT NULL,
  foreign2 NUMERIC NOT NULL,
  foreignNew numeric,
  FOREIGN KEY (foreign1, foreign2) REFERENCES ptdm_o.L2_CONSUMER (destKey1, destKey2),
  FOREIGN KEY (foreignNew) REFERENCES ptdm_o.L4_CONSUMER (newKey)
);

truncate table ptdm_o.L1_CONSUMER;
truncate table ptdm_o.L2_CONSUMER;
truncate table ptdm_o.L3_CONSUMER;
truncate table ptdm_o.L4_CONSUMER;


insert into ptdm_o.L3_CONSUMER values(4, 'Lil Mike', 'key4');
insert into ptdm_o.L3_CONSUMER values(5, 'Lil Steve', 'key5');
insert into ptdm_o.L3_CONSUMER values(6, 'Lil Teddy', 'key6');

insert into ptdm_o.L2_CONSUMER values(1, 1, 'Mike', 'key1',4);
insert into ptdm_o.L2_CONSUMER values(2, 2, 'Steve', 'key2',5);
insert into ptdm_o.L2_CONSUMER values(3, 3, 'Teddy', 'key3',6);

insert into ptdm_o.L4_CONSUMER values('Big Ol Mike', 11);
insert into ptdm_o.L4_CONSUMER values('Big Ol Steve', 22);
insert into ptdm_o.L4_CONSUMER values('Big Ol Teddy', 33);

insert into ptdm_o.L1_CONSUMER values('Big Mike', 1, 1, 1, 11);
insert into ptdm_o.L1_CONSUMER values('Big Steve', 2, 2, 2, 22);
insert into ptdm_o.L1_CONSUMER values('Big Teddy', 3, 3, 3, 33);