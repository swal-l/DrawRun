import os
from supabase import create_client, Client

def get_supabase_client() -> Client:
    url: str = os.environ.get("SUPABASE_URL")
    key: str = os.environ.get("SUPABASE_KEY")
    
    if not url or not key:
        print("Error: SUPABASE_URL or SUPABASE_KEY not found in environment variables.")
        return None
        
    return create_client(url, key)

def save_activity(user_email, activity_data):
    """
    Saves or updates an activity in Supabase.
    First finds the user by email, then inserts activity.
    """
    db = get_supabase_client()
    if not db:
        return {"error": "Database not configured"}
        
    # 1. Find or Create User (Simplified logic)
    # In a real app, user creation happens at signup. Here we auto-create for simplicity of the proxy.
    user_res = db.table("users").select("id").eq("email", user_email).execute()
    user_id = None
    
    if len(user_res.data) > 0:
        user_id = user_res.data[0]['id']
    else:
        # Create user
        new_user = db.table("users").insert({"email": user_email}).execute()
        if len(new_user.data) > 0:
            user_id = new_user.data[0]['id']
            
    if not user_id:
        return {"error": "Could not identify user"}

    # 2. Insert Activity
    # Check if exists by external_id (to avoid duplicates)
    external_id = str(activity_data.get("activityId"))
    existing = db.table("activities").select("id").eq("external_id", external_id).execute()
    
    if len(existing.data) == 0:
        payload = {
            "user_id": user_id,
            "external_id": external_id,
            "source": "garmin",
            "title": activity_data.get("activityName"),
            "type": activity_data.get("activityType", {}).get("typeKey"),
            "start_time": activity_data.get("startTimeLocal"),
            "distance_meters": activity_data.get("distance"),
            "duration_seconds": activity_data.get("duration"),
            "average_speed_mps": activity_data.get("averageSpeed"),
            "raw_data": activity_data # Store full JSON
        }
        db.table("activities").insert(payload).execute()
        return {"status": "saved"}
    else:
        return {"status": "skipped", "reason": "already_exists"}
