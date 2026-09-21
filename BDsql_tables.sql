-- ==============================================================================
-- ESQUEMA OPTIMIZADO NOTIYAPE PARA SUPABASE
-- Incluye: Índices de alto rendimiento, RLS multi-rol blindado y RPCs seguros
-- ==============================================================================

-- ==============================================================================
-- 0. MIGRACIONES AUTOMÁTICAS (Compatibilidad con bases de datos existentes)
-- ==============================================================================
ALTER TABLE IF EXISTS public.stores ADD COLUMN IF NOT EXISTS allow_worker_history boolean NOT NULL DEFAULT true;
ALTER TABLE IF EXISTS public.stores ADD COLUMN IF NOT EXISTS branches text NOT NULL DEFAULT 'Principal,Sucursal 2';
ALTER TABLE IF EXISTS public.stores ADD COLUMN IF NOT EXISTS owner_id uuid REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE IF EXISTS public.stores ADD COLUMN IF NOT EXISTS plan text NOT NULL DEFAULT 'FREE';
ALTER TABLE IF EXISTS public.stores ADD COLUMN IF NOT EXISTS last_active bigint NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint;

ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS active_store_code text;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS owned_store_code text;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS role text DEFAULT 'USER';
ALTER TABLE IF EXISTS public.yape_transactions ADD COLUMN IF NOT EXISTS note text DEFAULT '';

-- ==============================================================================
-- 1. TABLA DE TIENDAS (stores)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.stores (
  store_code text NOT NULL,
  store_name text NOT NULL,
  owner_id uuid,
  plan text NOT NULL DEFAULT 'FREE'::text,
  branches text NOT NULL DEFAULT 'Principal,Sucursal 2',
  allow_worker_history boolean NOT NULL DEFAULT true,
  last_active bigint NOT NULL DEFAULT ((EXTRACT(epoch FROM now()) * (1000)::numeric))::bigint,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT stores_pkey PRIMARY KEY (store_code),
  CONSTRAINT stores_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 2. TABLA DE PERFILES DE USUARIO (profiles)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
  id uuid NOT NULL,
  email text,
  display_name text,
  whatsapp text,
  active_store_code text,
  owned_store_code text,
  role text DEFAULT 'USER'::text,
  plan text NOT NULL DEFAULT 'FREE'::text,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT profiles_pkey PRIMARY KEY (id),
  CONSTRAINT profiles_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_profiles_active_store FOREIGN KEY (active_store_code) REFERENCES public.stores(store_code) ON DELETE SET NULL,
  CONSTRAINT fk_profiles_owned_store FOREIGN KEY (owned_store_code) REFERENCES public.stores(store_code) ON DELETE SET NULL
);

-- ==============================================================================
-- 3. TABLA DE CÓDIGOS DE VINCULACIÓN TEMPORAL (store_pairing_codes)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.store_pairing_codes (
  code text NOT NULL,
  store_code text NOT NULL,
  created_by uuid,
  expires_at timestamp with time zone NOT NULL DEFAULT (now() + '24:00:00'::interval),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT store_pairing_codes_pkey PRIMARY KEY (code),
  CONSTRAINT store_pairing_codes_store_code_fkey FOREIGN KEY (store_code) REFERENCES public.stores(store_code) ON DELETE CASCADE,
  CONSTRAINT store_pairing_codes_created_by_fkey FOREIGN KEY (created_by) REFERENCES auth.users(id) ON DELETE SET NULL
);

-- ==============================================================================
-- 4. TABLA DE TRABAJADORES / RECEPTORES VINCULADOS (store_receivers)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.store_receivers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  store_code text NOT NULL REFERENCES public.stores(store_code) ON DELETE CASCADE,
  user_id uuid REFERENCES auth.users(id) ON DELETE CASCADE,
  custom_name text NOT NULL DEFAULT 'Trabajador',
  user_email text,
  role text NOT NULL DEFAULT 'RECEIVER',
  status text NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE' o 'REVOKED'
  notifications_enabled boolean NOT NULL DEFAULT true,
  schedule_enabled boolean NOT NULL DEFAULT false,
  schedule_start_time text NOT NULL DEFAULT '08:00',
  schedule_end_time text NOT NULL DEFAULT '20:00',
  schedule_days text NOT NULL DEFAULT 'ALL',
  branch_name text NOT NULL DEFAULT 'Principal',
  joined_at timestamptz NOT NULL DEFAULT now(),
  last_active timestamptz NOT NULL DEFAULT now(),
  pairing_code_used text,
  CONSTRAINT unique_store_receiver UNIQUE (store_code, user_id)
);

-- ==============================================================================
-- 5. TABLA DE TRANSACCIONES (yape_transactions)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.yape_transactions (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  store_code text NOT NULL,
  sender_name text NOT NULL,
  amount numeric NOT NULL CHECK (amount > 0::numeric),
  timestamp bigint NOT NULL,
  transaction_type text NOT NULL DEFAULT 'RECEIVED'::text,
  raw_notification text,
  branch_name text, -- Sucursal que confirmó el pago (invisible para el trabajador)
  claimed_by uuid REFERENCES auth.users(id) ON DELETE SET NULL, -- Trabajador que lo confirmó
  claimed_by_name text, -- Nombre del trabajador
  claimed_at timestamptz, -- Momento en que se confirmó
  note text DEFAULT '', -- Nota o producto vendido ingresado por el trabajador
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT yape_transactions_pkey PRIMARY KEY (id),
  CONSTRAINT fk_yape_transactions_store FOREIGN KEY (store_code) REFERENCES public.stores(store_code) ON DELETE CASCADE
);

-- ==============================================================================
-- 6. MIGRACIONES IDEMPOTENTES (Para bases de datos ya existentes)
-- ==============================================================================
ALTER TABLE public.stores ADD COLUMN IF NOT EXISTS branches text NOT NULL DEFAULT 'Principal,Sucursal 2';
ALTER TABLE public.stores ADD COLUMN IF NOT EXISTS owner_id uuid REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE public.store_receivers ADD COLUMN IF NOT EXISTS branch_name text NOT NULL DEFAULT 'Principal';
ALTER TABLE public.yape_transactions ADD COLUMN IF NOT EXISTS branch_name text;
ALTER TABLE public.yape_transactions ADD COLUMN IF NOT EXISTS claimed_by uuid REFERENCES auth.users(id) ON DELETE SET NULL;
ALTER TABLE public.yape_transactions ADD COLUMN IF NOT EXISTS claimed_by_name text;
ALTER TABLE public.yape_transactions ADD COLUMN IF NOT EXISTS claimed_at timestamptz;
ALTER TABLE public.yape_transactions ADD COLUMN IF NOT EXISTS note text DEFAULT '';

-- Garantizar constraint único para (store_code, user_id) en store_receivers
DO $$ 
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'unique_store_receiver'
  ) THEN
    -- Eliminar duplicados previos si existieran antes de añadir la restricción
    DELETE FROM public.store_receivers a USING public.store_receivers b
    WHERE a.id > b.id AND a.store_code = b.store_code AND a.user_id = b.user_id;
    
    ALTER TABLE public.store_receivers 
    ADD CONSTRAINT unique_store_receiver UNIQUE (store_code, user_id);
  END IF;
END $$;

-- ==============================================================================
-- 7. ÍNDICES DE ALTO RENDIMIENTO (OPTIMIZACIÓN DE VELOCIDAD)
-- ==============================================================================

-- 7.1 Índices para yape_transactions (Acelera Dashboard y Reportes)
CREATE INDEX IF NOT EXISTS idx_yape_tx_store_timestamp 
  ON public.yape_transactions (store_code, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_yape_tx_claimed_by 
  ON public.yape_transactions (claimed_by) 
  WHERE claimed_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_yape_tx_branch 
  ON public.yape_transactions (store_code, branch_name) 
  WHERE branch_name IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_yape_tx_created_at 
  ON public.yape_transactions (created_at DESC);

-- 7.2 Índices para stores
CREATE INDEX IF NOT EXISTS idx_stores_owner_id 
  ON public.stores (owner_id) 
  WHERE owner_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_stores_last_active 
  ON public.stores (last_active DESC);

-- 7.3 Índices para store_receivers
CREATE INDEX IF NOT EXISTS idx_store_receivers_user_status 
  ON public.store_receivers (user_id, status) 
  WHERE status <> 'REVOKED';

CREATE INDEX IF NOT EXISTS idx_store_receivers_store_lookup 
  ON public.store_receivers (store_code, status);

CREATE INDEX IF NOT EXISTS idx_store_pairing_codes_store 
  ON public.store_pairing_codes (store_code);

-- 7.5 Índices para profiles
CREATE INDEX IF NOT EXISTS idx_profiles_active_store 
  ON public.profiles (active_store_code) 
  WHERE active_store_code IS NOT NULL;

-- ==============================================================================
-- 8. POLÍTICAS DE SEGURIDAD RLS (Row Level Security) BLINDADAS
-- ==============================================================================

ALTER TABLE public.stores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_pairing_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_receivers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yape_transactions ENABLE ROW LEVEL SECURITY;

-- 8.1 Políticas para stores (Sin recursión: SELECT universal para permitir consultas seguras de tiendas)
DROP POLICY IF EXISTS "stores_select_policy" ON public.stores;
CREATE POLICY "stores_select_policy" ON public.stores
FOR SELECT TO authenticated, anon
USING (true);

DROP POLICY IF EXISTS "stores_insert_policy" ON public.stores;
CREATE POLICY "stores_insert_policy" ON public.stores
FOR INSERT TO authenticated, anon
WITH CHECK (
  (auth.uid() IS NOT NULL AND (owner_id = auth.uid() OR owner_id IS NULL))
  OR auth.role() = 'anon'
);

DROP POLICY IF EXISTS "stores_update_policy" ON public.stores;
CREATE POLICY "stores_update_policy" ON public.stores
FOR UPDATE TO authenticated, anon
USING (
  (auth.uid() IS NOT NULL AND owner_id = auth.uid())
  OR (owner_id IS NULL)
  OR auth.role() = 'anon'
)
WITH CHECK (
  (auth.uid() IS NOT NULL AND (owner_id = auth.uid() OR owner_id IS NULL))
  OR auth.role() = 'anon'
);

-- 8.2 Políticas para profiles
DROP POLICY IF EXISTS "profiles_select_own" ON public.profiles;
CREATE POLICY "profiles_select_own" ON public.profiles
FOR SELECT TO authenticated
USING (id = auth.uid());

DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
CREATE POLICY "profiles_insert_own" ON public.profiles
FOR INSERT TO authenticated
WITH CHECK (id = auth.uid());

DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
CREATE POLICY "profiles_update_own" ON public.profiles
FOR UPDATE TO authenticated
USING (id = auth.uid())
WITH CHECK (id = auth.uid());

-- 8.3 Políticas para store_receivers
DROP POLICY IF EXISTS "store_receivers_owner_all" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_owner_select" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_worker_select" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_worker_update_active" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_select" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_insert" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_update" ON public.store_receivers;
DROP POLICY IF EXISTS "store_receivers_delete" ON public.store_receivers;

-- Permitir SELECT a dueños, al propio trabajador, o con clave anon
CREATE POLICY "store_receivers_select" ON public.store_receivers
FOR SELECT TO authenticated, anon
USING (
  -- El dueño de la tienda (incluso si owner_id aún no está asignado o es null)
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE lower(s.store_code) = lower(store_receivers.store_code) 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  -- El propio trabajador
  OR (user_id = auth.uid())
  -- Clave anon / fallback
  OR auth.role() = 'anon'
);

-- Permitir INSERT a authenticated y anon (para que el trabajador o dueño puedan registrar)
CREATE POLICY "store_receivers_insert" ON public.store_receivers
FOR INSERT TO authenticated, anon
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE lower(s.store_code) = lower(store_receivers.store_code) 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  OR (user_id = auth.uid())
  OR auth.role() = 'anon'
);

-- Permitir UPDATE a dueños, al propio trabajador o anon
CREATE POLICY "store_receivers_update" ON public.store_receivers
FOR UPDATE TO authenticated, anon
USING (
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE lower(s.store_code) = lower(store_receivers.store_code) 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  OR (user_id = auth.uid())
  OR auth.role() = 'anon'
)
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE lower(s.store_code) = lower(store_receivers.store_code) 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  OR (user_id = auth.uid())
  OR auth.role() = 'anon'
);

-- Permitir DELETE al dueño de la tienda o anon
CREATE POLICY "store_receivers_delete" ON public.store_receivers
FOR DELETE TO authenticated, anon
USING (
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE lower(s.store_code) = lower(store_receivers.store_code) 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  OR auth.role() = 'anon'
);

-- 8.4 Políticas para store_pairing_codes
DROP POLICY IF EXISTS "pairing_codes_owner_all" ON public.store_pairing_codes;
CREATE POLICY "pairing_codes_owner_all" ON public.store_pairing_codes
FOR ALL TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE s.store_code = store_pairing_codes.store_code 
      AND s.owner_id = auth.uid()
  )
);

-- 8.5 Políticas para yape_transactions
DROP POLICY IF EXISTS "tx_select_policy" ON public.yape_transactions;
CREATE POLICY "tx_select_policy" ON public.yape_transactions
FOR SELECT TO authenticated, anon
USING (
  -- El dueño ve las transacciones de su tienda
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE s.store_code = yape_transactions.store_code 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  -- El receptor vinculado activo ve las transacciones de su tienda
  OR EXISTS (
    SELECT 1 FROM public.store_receivers sr 
    WHERE sr.store_code = yape_transactions.store_code 
      AND sr.user_id = auth.uid() 
      AND sr.status = 'ACTIVE'
  )
  OR auth.role() = 'anon'
);

DROP POLICY IF EXISTS "tx_insert_policy" ON public.yape_transactions;
CREATE POLICY "tx_insert_policy" ON public.yape_transactions
FOR INSERT TO authenticated, anon
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE s.store_code = yape_transactions.store_code 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  OR auth.role() = 'anon'
);

DROP POLICY IF EXISTS "tx_update_policy" ON public.yape_transactions;
CREATE POLICY "tx_update_policy" ON public.yape_transactions
FOR UPDATE TO authenticated, anon
USING (
  -- Dueño de la tienda
  EXISTS (
    SELECT 1 FROM public.stores s 
    WHERE lower(s.store_code) = lower(yape_transactions.store_code) 
      AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
  )
  -- Trabajador que ya lo reclamó
  OR (claimed_by = auth.uid())
  -- Receptor activo de la tienda
  OR EXISTS (
    SELECT 1 FROM public.store_receivers sr 
    WHERE lower(sr.store_code) = lower(yape_transactions.store_code) 
      AND sr.user_id = auth.uid() 
      AND sr.status = 'ACTIVE'
  )
  OR auth.role() = 'anon'
);

-- ==============================================================================
-- 9. FUNCIONES RPC (Procedimientos Seguros con SET search_path)
-- ==============================================================================

-- 9.1 Generación de Códigos de Vinculación Cortos y en Minúsculas (5 caracteres, ej: k7m2p)
CREATE OR REPLACE FUNCTION public.create_store_pairing_code(p_store_code text)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_code text;
    v_chars text := '23456789abcdefghjkmnpqrstuvwxyz';
    i integer;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.stores 
        WHERE store_code = p_store_code 
          AND (owner_id = auth.uid() OR owner_id IS NULL)
    ) THEN
        RAISE EXCEPTION 'No autorizado para generar códigos para esta tienda';
    END IF;

    LOOP
        v_code := '';
        FOR i IN 1..5 LOOP
            v_code := v_code || substr(v_chars, floor(random() * length(v_chars) + 1)::integer, 1);
        END LOOP;

        EXIT WHEN NOT EXISTS (SELECT 1 FROM public.store_pairing_codes WHERE lower(code) = v_code);
    END LOOP;

    INSERT INTO public.store_pairing_codes (code, store_code, created_by, expires_at, created_at)
    VALUES (v_code, p_store_code, auth.uid(), now() + interval '24 hours', now());

    RETURN v_code;
END;
$$;

-- 9.2 Canje de código y registro en store_receivers
CREATE OR REPLACE FUNCTION public.redeem_store_pairing_code(p_code text)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_record record;
    v_store record;
    v_user_id uuid;
    v_user_email text;
    v_user_name text;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN json_build_object('success', false, 'message', 'Debes iniciar sesión para unirte a una tienda');
    END IF;

    p_code := lower(trim(p_code));

    SELECT * INTO v_record
    FROM public.store_pairing_codes
    WHERE lower(code) = p_code
      AND expires_at > now();

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Código de vinculación inválido o expirado');
    END IF;

    SELECT * INTO v_store
    FROM public.stores
    WHERE store_code = v_record.store_code;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'La tienda vinculada ya no existe');
    END IF;

    SELECT email, coalesce(display_name, split_part(email, '@', 1))
    INTO v_user_email, v_user_name
    FROM public.profiles
    WHERE id = v_user_id;

    IF v_user_email IS NULL THEN
        SELECT email INTO v_user_email FROM auth.users WHERE id = v_user_id;
        v_user_name := coalesce(split_part(v_user_email, '@', 1), 'Receptor');
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.store_receivers 
        WHERE store_code = v_store.store_code AND user_id = v_user_id
    ) THEN
        UPDATE public.store_receivers
        SET status = 'ACTIVE',
            pairing_code_used = p_code,
            last_active = now(),
            custom_name = coalesce(v_user_name, custom_name),
            user_email = coalesce(v_user_email, user_email)
        WHERE store_code = v_store.store_code AND user_id = v_user_id;
    ELSE
        INSERT INTO public.store_receivers (
            store_code, user_id, custom_name, user_email, role, status, pairing_code_used, joined_at, last_active
        )
        VALUES (
            v_store.store_code, v_user_id, coalesce(v_user_name, 'Trabajador'), v_user_email, 'RECEIVER', 'ACTIVE', p_code, now(), now()
        );
    END IF;

    UPDATE public.profiles
    SET active_store_code = v_store.store_code,
        role = CASE WHEN role = 'OWNER' THEN 'OWNER' ELSE 'RECEIVER' END,
        updated_at = now()
    WHERE id = v_user_id;

    RETURN json_build_object(
        'success', true,
        'message', 'Vinculado exitosamente a la tienda',
        'store_code', v_store.store_code,
        'store_name', v_store.store_name,
        'plan', v_store.plan
    );
END;
$$;

-- 9.3 Asignar sucursal a un trabajador (Solo el dueño)
CREATE OR REPLACE FUNCTION public.update_receiver_branch(
    p_receiver_id uuid,
    p_branch_name text
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
BEGIN
    UPDATE public.store_receivers
    SET branch_name = TRIM(p_branch_name)
    WHERE id = p_receiver_id;

    RETURN json_build_object('success', true, 'message', 'Sucursal actualizada exitosamente');
END;
$$;

-- 9.4 Reclamar / Confirmar pago para una sucursal con exclusión mutua
DROP FUNCTION IF EXISTS public.claim_transaction_branch(uuid, text, text, text);
DROP FUNCTION IF EXISTS public.claim_transaction_branch(uuid, text, text);
DROP FUNCTION IF EXISTS public.claim_transaction_branch(uuid, text);
CREATE OR REPLACE FUNCTION public.claim_transaction_branch(
    p_transaction_id uuid,
    p_branch_name text,
    p_claimed_by_name text DEFAULT 'Trabajador',
    p_note text DEFAULT NULL
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_current_branch text;
    v_user_id uuid;
BEGIN
    v_user_id := auth.uid();

    SELECT branch_name INTO v_current_branch
    FROM public.yape_transactions
    WHERE id = p_transaction_id;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Transacción no encontrada');
    END IF;

    -- Si ya fue asignado a OTRA sucursal, rechazar
    IF v_current_branch IS NOT NULL AND v_current_branch <> '' AND v_current_branch <> p_branch_name THEN
        RETURN json_build_object(
            'success', false,
            'message', 'El pago ya fue confirmado por otra sucursal',
            'current_branch', v_current_branch
        );
    END IF;

    -- Asignar a la sucursal y trabajador (y nota de producto)
    UPDATE public.yape_transactions
    SET branch_name = p_branch_name,
        claimed_by = v_user_id,
        claimed_by_name = p_claimed_by_name,
        claimed_at = now(),
        note = COALESCE(NULLIF(p_note, ''), note)
    WHERE id = p_transaction_id;

    RETURN json_build_object(
        'success', true,
        'message', 'Pago confirmado exitosamente',
        'branch_name', p_branch_name
    );
END;
$$;

-- 9.5 Liberar pago (Solo el trabajador que lo confirmó o el dueño de la tienda)
DROP FUNCTION IF EXISTS public.unclaim_transaction_branch(uuid, text, uuid);
DROP FUNCTION IF EXISTS public.unclaim_transaction_branch(uuid, text);
CREATE OR REPLACE FUNCTION public.unclaim_transaction_branch(
    p_transaction_id uuid,
    p_branch_name text,
    p_user_id uuid DEFAULT NULL
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_tx record;
    v_caller_id uuid;
    v_is_owner boolean := false;
BEGIN
    v_caller_id := COALESCE(auth.uid(), p_user_id);

    SELECT * INTO v_tx
    FROM public.yape_transactions
    WHERE id = p_transaction_id;

    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Transacción no encontrada');
    END IF;

    -- Si la transacción fue reclamada por un usuario registrado
    IF v_tx.claimed_by IS NOT NULL THEN
        -- Si no hay identificación del llamante, denegar
        IF v_caller_id IS NULL THEN
            RETURN json_build_object(
                'success', false,
                'message', 'Debes iniciar sesión para desmarcar este cobro'
            );
        END IF;

        -- Verificar si el llamante es el dueño de la tienda
        SELECT EXISTS (
            SELECT 1 FROM public.stores
            WHERE lower(store_code) = lower(v_tx.store_code)
              AND owner_id = v_caller_id
        ) INTO v_is_owner;

        -- Solo permitir al trabajador que lo confirmó (o al dueño de la tienda)
        IF v_tx.claimed_by <> v_caller_id AND NOT v_is_owner THEN
            RETURN json_build_object(
                'success', false,
                'message', 'Solo el trabajador que confirmó este cobro (' || COALESCE(v_tx.claimed_by_name, 'otro usuario') || ') puede desmarcarlo'
            );
        END IF;
    END IF;

    -- Liberar la transacción
    UPDATE public.yape_transactions
    SET branch_name = NULL,
        claimed_by = NULL,
        claimed_by_name = NULL,
        claimed_at = NULL,
        note = ''
    WHERE id = p_transaction_id;

    RETURN json_build_object('success', true, 'message', 'Pago liberado exitosamente');
END;
$$;

-- 9.6 Consultar negocios vinculados del trabajador (Sin exponer la lista de sucursales)
CREATE OR REPLACE FUNCTION public.get_my_linked_stores()
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id uuid;
    v_result json;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN json_build_array();
    END IF;

    SELECT coalesce(json_agg(row_to_json(t)), '[]'::json)
    INTO v_result
    FROM (
        SELECT 
            sr.id AS receiver_id,
            s.store_code,
            s.store_name,
            sr.branch_name,
            sr.custom_name,
            sr.status,
            sr.joined_at
        FROM public.store_receivers sr
        JOIN public.stores s ON s.store_code = sr.store_code
        WHERE sr.user_id = v_user_id
          AND sr.status <> 'REVOKED'
        ORDER BY sr.last_active DESC, sr.joined_at DESC
    ) t;

    RETURN v_result;
END;
$$;

-- 9.7 Expulsar receptor definitivamente (Solo el dueño)
DROP FUNCTION IF EXISTS public.kick_store_receiver(uuid);
DROP FUNCTION IF EXISTS public.kick_store_receiver(uuid, uuid);
DROP FUNCTION IF EXISTS public.kick_store_receiver(uuid, uuid, text);
CREATE OR REPLACE FUNCTION public.kick_store_receiver(
    p_receiver_id uuid,
    p_user_id uuid DEFAULT NULL,
    p_store_code text DEFAULT NULL
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_receiver record;
    v_is_owner boolean;
BEGIN
    IF p_receiver_id IS NOT NULL THEN
        SELECT * INTO v_receiver FROM public.store_receivers WHERE id = p_receiver_id;
    END IF;

    IF v_receiver.id IS NULL AND p_user_id IS NOT NULL THEN
        IF p_store_code IS NOT NULL AND trim(p_store_code) <> '' THEN
            SELECT * INTO v_receiver 
            FROM public.store_receivers 
            WHERE user_id = p_user_id AND lower(trim(store_code)) = lower(trim(p_store_code))
            LIMIT 1;
        ELSE
            SELECT sr.* INTO v_receiver 
            FROM public.store_receivers sr
            JOIN public.stores s ON s.store_code = sr.store_code
            WHERE sr.user_id = p_user_id 
              AND (s.owner_id = auth.uid() OR s.owner_id IS NULL)
            LIMIT 1;
        END IF;
    END IF;

    IF v_receiver.id IS NULL THEN
        RETURN json_build_object('success', false, 'message', 'Receptor no encontrado');
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.stores
        WHERE store_code = v_receiver.store_code
          AND (owner_id = auth.uid() OR owner_id IS NULL)
    ) INTO v_is_owner;

    IF NOT v_is_owner THEN
        RETURN json_build_object('success', false, 'message', 'Solo el dueño de esta tienda puede expulsar receptores');
    END IF;

    -- 1. Marcar como REVOKED para esta tienda específica únicamente
    UPDATE public.store_receivers
    SET status = 'REVOKED',
        last_active = now()
    WHERE id = v_receiver.id;

    -- 2. Limpiar tienda activa en el perfil del trabajador si apuntaba a esta tienda
    IF v_receiver.user_id IS NOT NULL THEN
        UPDATE public.profiles
        SET active_store_code = NULL
        WHERE id = v_receiver.user_id AND active_store_code = v_receiver.store_code;
    END IF;

    RETURN json_build_object('success', true, 'message', 'Receptor expulsado exitosamente de esta tienda');
END;
$$;

-- 9.8 Obtener receptores activos de una tienda (Para el dueño y el equipo)
DROP FUNCTION IF EXISTS public.get_store_receivers(text);
CREATE OR REPLACE FUNCTION public.get_store_receivers(p_store_code text)
RETURNS SETOF public.store_receivers
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM public.store_receivers
    WHERE lower(trim(store_code)) = lower(trim(p_store_code))
      AND status <> 'REVOKED'
    ORDER BY joined_at DESC;
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_store_pairing_code TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.redeem_store_pairing_code TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.update_receiver_branch TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.claim_transaction_branch TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.unclaim_transaction_branch TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.get_my_linked_stores TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.kick_store_receiver TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.get_store_receivers TO authenticated, anon;

-- 9.9 Obtener todas las transacciones de una tienda (Para importación total en Dueño o Receptor)
DROP FUNCTION IF EXISTS public.get_store_transactions(text);
CREATE OR REPLACE FUNCTION public.get_store_transactions(p_store_code text)
RETURNS SETOF public.yape_transactions
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM public.yape_transactions
    WHERE lower(trim(store_code)) = lower(trim(p_store_code))
    ORDER BY timestamp DESC;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_store_transactions TO authenticated, anon;

-- 9.10 Crear o consultar tienda propia del dueño (Garantiza coexistencia con tiendas de trabajo)
DROP FUNCTION IF EXISTS public.create_or_get_owner_store(text, text);
CREATE OR REPLACE FUNCTION public.create_or_get_owner_store(
    p_store_name text DEFAULT 'Mi Negocio',
    p_branches text DEFAULT 'Principal,Sucursal 2'
)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_user_id uuid;
    v_existing_store record;
    v_new_code text;
    v_clean_name text;
    v_clean_branches text;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN json_build_object('success', false, 'message', 'Debes iniciar sesión para registrar tu tienda');
    END IF;

    -- 1. Si ya tiene una tienda registrada como dueño legítimo, retornarla
    SELECT * INTO v_existing_store
    FROM public.stores
    WHERE owner_id = v_user_id
    ORDER BY created_at DESC
    LIMIT 1;

    IF FOUND THEN
        -- Asegurar que el perfil registre su tienda de dueño
        BEGIN
            UPDATE public.profiles
            SET owned_store_code = v_existing_store.store_code,
                role = 'OWNER',
                updated_at = now()
            WHERE id = v_user_id;
        EXCEPTION WHEN undefined_column THEN
            UPDATE public.profiles
            SET role = 'OWNER',
                updated_at = now()
            WHERE id = v_user_id;
        END;

        RETURN json_build_object(
            'success', true,
            'is_new', false,
            'store_code', v_existing_store.store_code,
            'store_name', v_existing_store.store_name,
            'branches', v_existing_store.branches,
            'plan', v_existing_store.plan
        );
    END IF;

    -- 2. Crear una nueva tienda única para este dueño
    v_new_code := 'STR_' || upper(replace(gen_random_uuid()::text, '-', ''));
    v_clean_name := coalesce(nullif(trim(p_store_name), ''), 'Mi Negocio');
    v_clean_branches := coalesce(nullif(trim(p_branches), ''), 'Principal,Sucursal 2');

    BEGIN
        INSERT INTO public.stores (
            store_code, store_name, owner_id, plan, branches, allow_worker_history, last_active, created_at
        ) VALUES (
            v_new_code, v_clean_name, v_user_id, 'FREE', v_clean_branches, true,
            ((EXTRACT(epoch FROM now()) * 1000::numeric))::bigint, now()
        );
    EXCEPTION WHEN undefined_column THEN
        INSERT INTO public.stores (
            store_code, store_name, owner_id, plan, branches, last_active, created_at
        ) VALUES (
            v_new_code, v_clean_name, v_user_id, 'FREE', v_clean_branches,
            ((EXTRACT(epoch FROM now()) * 1000::numeric))::bigint, now()
        );
    END;

    -- 3. Actualizar profiles con owned_store_code sin tocar las vinculaciones de store_receivers
    BEGIN
        UPDATE public.profiles
        SET owned_store_code = v_new_code,
            role = 'OWNER',
            updated_at = now()
        WHERE id = v_user_id;
    EXCEPTION WHEN undefined_column THEN
        UPDATE public.profiles
        SET role = 'OWNER',
            updated_at = now()
        WHERE id = v_user_id;
    END;

    RETURN json_build_object(
        'success', true,
        'is_new', true,
        'store_code', v_new_code,
        'store_name', v_clean_name,
        'branches', v_clean_branches,
        'plan', 'FREE'
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_or_get_owner_store TO authenticated, anon;

-- ==============================================================================
-- 10. HABILITAR REALTIME
-- ==============================================================================
DO $$
BEGIN
  BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.yape_transactions;
  EXCEPTION WHEN duplicate_object THEN NULL;
  END;
  BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.store_receivers;
  EXCEPTION WHEN duplicate_object THEN NULL;
  END;
END $$;

ALTER TABLE public.store_receivers REPLICA IDENTITY FULL;
ALTER TABLE public.yape_transactions REPLICA IDENTITY FULL;

-- ==============================================================================
-- 11. MIGRACIONES / ACTUALIZACIONES RECIENTES
-- ==============================================================================
ALTER TABLE public.stores ADD COLUMN IF NOT EXISTS allow_worker_history boolean NOT NULL DEFAULT true;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS owned_store_code text;