-- ==============================================================================
-- MIGRACIÓN SUPABASE: TABLA DE BETA TESTERS (beta_testers)
-- Ejecuta este script en el "SQL Editor" de tu Dashboard de Supabase.
-- ==============================================================================

-- 1. Crear tabla de beta testers
CREATE TABLE IF NOT EXISTS public.beta_testers (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL DEFAULT 'Tester Google Play',
  email text NOT NULL,
  device_model text DEFAULT 'Web Form',
  status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'INVITED', 'ACTIVE', 'REVOKED')),
  notes text DEFAULT '',
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT unique_beta_tester_email UNIQUE (email)
);

-- 2. Habilitar Seguridad por Fila (RLS)
ALTER TABLE public.beta_testers ENABLE ROW LEVEL SECURITY;

-- 3. Políticas de acceso (RLS)
DROP POLICY IF EXISTS "Permitir registro de beta testers" ON public.beta_testers;
CREATE POLICY "Permitir registro de beta testers"
  ON public.beta_testers FOR INSERT
  TO anon, authenticated
  WITH CHECK (true);

DROP POLICY IF EXISTS "Permitir ver beta testers" ON public.beta_testers;
CREATE POLICY "Permitir ver beta testers"
  ON public.beta_testers FOR SELECT
  TO anon, authenticated
  USING (true);

DROP POLICY IF EXISTS "Permitir actualizacion de beta testers" ON public.beta_testers;
CREATE POLICY "Permitir actualizacion de beta testers"
  ON public.beta_testers FOR UPDATE
  TO anon, authenticated
  USING (true);

-- 4. Otorgar permisos a los roles de Supabase
GRANT SELECT, INSERT, UPDATE ON public.beta_testers TO anon, authenticated;

-- 5. Índices de rendimiento
CREATE INDEX IF NOT EXISTS idx_beta_testers_email ON public.beta_testers (email);
CREATE INDEX IF NOT EXISTS idx_beta_testers_status_created ON public.beta_testers (status, created_at DESC);

-- 6. Vista para exportar o revisar directamente la lista para Google Play Console
-- Se especifica security_invoker = true para respetar las políticas de RLS según las directrices de Supabase
CREATE OR REPLACE VIEW public.v_beta_testers_play_list 
WITH (security_invoker = true) AS
SELECT 
  email,
  name,
  status,
  device_model,
  created_at
FROM public.beta_testers
ORDER BY created_at DESC;

-- 7. Migración automática de registros previos desde 'beta_feedback' (si existieran)
DO $$ 
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'beta_feedback') THEN
    INSERT INTO public.beta_testers (email, name, device_model, notes, created_at)
    SELECT 
      email,
      COALESCE(name, 'Tester Google Play'),
      COALESCE(device_model, 'Web Form'),
      COALESCE(message, 'Migrado desde beta_feedback'),
      created_at
    FROM public.beta_feedback
    WHERE (feedback_type = 'tester_signup' OR message ILIKE '%prueba interna Google Play%')
      AND email IS NOT NULL AND email <> ''
    ON CONFLICT (email) DO UPDATE 
    SET updated_at = now();
  END IF;
END $$;

-- ==============================================================================
-- CONSULTAS ÚTILES PARA EL ADMINISTRADOR (Copiar y pegar según necesidad)
-- ==============================================================================

-- A) Ver todos los testers registrados ordenados por el más reciente:
-- SELECT * FROM public.beta_testers ORDER BY created_at DESC;

-- B) Copiar todos los correos Gmail separados por coma para pegar en Google Play Console:
-- SELECT string_agg(email, ', ') AS correos_para_google_play FROM public.beta_testers WHERE status != 'REVOKED';

-- C) Marcar un tester como invitado:
-- UPDATE public.beta_testers SET status = 'INVITED', updated_at = now() WHERE email = 'ejemplo@gmail.com';
