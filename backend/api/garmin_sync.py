from http.server import BaseHTTPRequestHandler
from garminconnect import Garmin
import json
import urllib.parse
from datetime import date, timedelta

class handler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        try:
            data = json.loads(post_data.decode('utf-8'))
            email = data.get('email')
            password = data.get('password')
            
            if not email or not password:
                self.send_response(400)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"error": "Missing credentials"}).encode('utf-8'))
                return

            # Init Garmin
            # Note: In a real serverless env, we might face MFA challenges.
            # garminconnect tries to handle login.
            client = Garmin(email, password)
            client.login()
            
            # Fetch last 10 activities
            activities = client.get_activities(0, 10)
            
            # Extract relevant fields for our App
            # Map standard Activity fields to our needs
            simplified_activities = []
            for act in activities:
                simplified_activities.append({
                    "id": str(act["activityId"]),
                    "name": act["activityName"],
                    "type": act["activityType"]["typeKey"],
                    "startTime": act["startTimeLocal"],
                    "distance": act["distance"],
                    "duration": act["duration"],
                    "avgSpeed": act.get("averageSpeed", 0.0),
                    "elevation": act.get("elevationGain", 0.0),
                    "avgHr": act.get("averageHR", 0),
                    # Add more fields as needed
                })

            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({
                "status": "success", 
                "message": f"Found {len(simplified_activities)} activities",
                "data": simplified_activities
            }).encode('utf-8'))
            
        except Exception as e:
            self.send_response(500)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            error_msg = str(e)
            self.wfile.write(json.dumps({"error": error_msg}).encode('utf-8'))
