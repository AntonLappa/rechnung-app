--liquibase formatted sql

--changeset antonlappa:012-clear-foreign-logo-paths
--comment: logo_path was previously client-settable; clear keys outside the owner's logos/{user_id}/ prefix. Bucket objects are left untouched.
UPDATE company_profiles
SET logo_path = NULL
WHERE logo_path IS NOT NULL
  AND logo_path NOT LIKE 'logos/' || user_id::text || '/%';
