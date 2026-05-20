# Supabase Setup

1. Create a Supabase project from the dashboard.
2. Copy the project URL and anon/publishable key.
3. Add these values to `local.properties`:

```properties
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-or-publishable-key
```

4. Use the shared client from Kotlin:

```kotlin
val supabase = SupabaseManager.client
```

From Java:

```java
SupabaseClient supabase = SupabaseManager.INSTANCE.getClient();
```

Supabase table access depends on Row Level Security policies. If reads or writes fail with permission errors, check the table policies in the Supabase dashboard.
