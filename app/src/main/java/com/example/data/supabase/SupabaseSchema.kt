package com.example.data.supabase

object SupabaseSchema {

    const val TABLE_TRANSACTIONS = "yape_transactions"
    const val TABLE_STORES = "stores"
    const val TABLE_PROFILES = "profiles"

    val SQL_SETUP_SCRIPT: String = """
-- 1. Tabla de Tiendas / Dispositivos (Se crea primero para permitir la Foreign Key)
CREATE TABLE IF NOT EXISTS public.stores (
  store_code text NOT NULL,
  store_name text NOT NULL,
  last_active bigint NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT stores_pkey PRIMARY KEY (store_code)
);

-- 2. Tabla Consolidada de Transacciones Yape / Plin
CREATE TABLE IF NOT EXISTS public.yape_transactions (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  store_code text NOT NULL,
  sender_name text NOT NULL,
  amount numeric(10, 2) NOT NULL CHECK (amount > 0),
  timestamp bigint NOT NULL,
  transaction_type text NOT NULL DEFAULT 'RECEIVED'::text,
  raw_notification text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  
  CONSTRAINT yape_transactions_pkey PRIMARY KEY (id),
  CONSTRAINT fk_yape_transactions_store FOREIGN KEY (store_code) 
    REFERENCES public.stores (store_code) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 3. Índices para Acelerar Consultas de la App Móvil
CREATE INDEX IF NOT EXISTS idx_yape_tx_store_timestamp 
  ON public.yape_transactions (store_code, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_yape_tx_created_at 
  ON public.yape_transactions (created_at DESC);

-- 4. Prevención de Transacciones Duplicadas
CREATE UNIQUE INDEX IF NOT EXISTS idx_prevent_duplicate_yape_tx 
  ON public.yape_transactions (store_code, timestamp, sender_name, amount);

-- 5. Configuración de Supabase: Row Level Security (RLS) y Realtime
ALTER TABLE public.stores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yape_transactions ENABLE ROW LEVEL SECURITY;

-- Políticas RLS para lectura y escritura de la App (Rol 'anon')
CREATE POLICY "Allow anon select stores" ON public.stores 
  FOR SELECT TO anon USING (true);

CREATE POLICY "Allow anon all stores" ON public.stores 
  FOR ALL TO anon USING (true) WITH CHECK (true);

CREATE POLICY "Allow anon select yape_transactions" ON public.yape_transactions 
  FOR SELECT TO anon USING (true);

CREATE POLICY "Allow anon insert yape_transactions" ON public.yape_transactions 
  FOR INSERT TO anon WITH CHECK (true);

CREATE POLICY "Allow anon delete yape_transactions" ON public.yape_transactions 
  FOR DELETE TO anon USING (true);

-- Habilitar la transmisión en tiempo real a los dispositivos
ALTER PUBLICATION supabase_realtime ADD TABLE public.stores;
ALTER PUBLICATION supabase_realtime ADD TABLE public.yape_transactions;
""".trimIndent()
}
