-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- Table: Users
create table public.users (
  id uuid default uuid_generate_v4() primary key,
  email text unique not null,
  full_name text,
  avatar_url text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Table: Activities (Stored from Garmin/Strava)
create table public.activities (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.users(id) on delete cascade not null,
  external_id text, -- ID from Garmin/Strava
  source text, -- 'garmin', 'strava', 'manual'
  title text,
  type text, -- 'run', 'swim', etc.
  start_time timestamp with time zone not null,
  distance_meters float,
  duration_seconds int,
  elevation_gain_meters float,
  average_speed_mps float,
  average_hr int,
  map_polyline text, -- For displaying the map
  raw_data jsonb, -- Store full JSON response just in case
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Table: Friendships (For Social Network)
create table public.friendships (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.users(id) not null,
  friend_id uuid references public.users(id) not null,
  status text check (status in ('pending', 'accepted', 'blocked')) default 'pending',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique(user_id, friend_id)
);

-- Post-REST Security (Row Level Security)
alter table public.users enable row level security;
alter table public.activities enable row level security;
alter table public.friendships enable row level security;

-- Policies (Simple Start: Read everyone, Write own)
-- Note: In a real app, you'd restrict Read to friends only.
create policy "Public profiles are viewable by everyone." on public.users for select using (true);
create policy "Users can insert their own profile." on public.users for insert with check (auth.uid() = id); -- Requires Supabase Auth
create policy "Users can update own profile." on public.users for update using (auth.uid() = id);

-- For now, allow server-side key (Service Role) to bypass, but for client-side:
-- (We will mostly use Server-Side logic from Vercel, which uses the Service Role Key, so it bypasses RLS)
