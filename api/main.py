from fastapi import FastAPI, File, UploadFile, HTTPException
from typing import List
import json
import cv2
import numpy as np
from datetime import datetime
import base64
from pydantic import BaseModel
from ultralytics import YOLO
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="WE Bridge UU API")

origins = ["*"]  # Allow requests from any origin

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

# Load YOLO model
def load_yolo_model():
    # model = YOLO("model/ppe.pt")
    model = YOLO("model/yolo11n.pt")
    return model

class DetectionResponse(BaseModel):
    detection_id: str
    timestamp: str
    # objects_detected: List[Dict[str, Any]]
    objects_detected: List[str]
    response_image: str



YOLO_MODEL = load_yolo_model()

# @app.on_event("startup")
# async def startup_event():
#     continue

def process_image(image_bytes):
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    results = YOLO_MODEL.predict(img, conf=0.5)
    detections = []
    for result in results:
        for box in result.boxes:
            cv2.rectangle(img, (int(box.xyxy[0][0]), int(box.xyxy[0][1])),
                          (int(box.xyxy[0][2]), int(box.xyxy[0][3])), (0, 0, 255), 3)
            cv2.putText(img, f"{result.names[int(box.cls[0])]}",
                        (int(box.xyxy[0][0]), int(box.xyxy[0][1]) - 10),
                        cv2.FONT_HERSHEY_PLAIN, 3, (0, 0, 255), 2)
            # detections.append((f"{result.names[int(box.cls[0])]}", box.conf.item()))
            detections.append(f"{result.names[int(box.cls[0])]}")
    
    # # Convert back to bytes for response
    _, annotated_buffer = cv2.imencode('.jpg', img)
    # annotated_bytes = annotated_buffer.tobytes()

    # convert the annotated image to Base64
    base64_image = base64.b64encode(annotated_buffer)
    
    return detections, base64_image

@app.post("/classify")
async def detect_objects(
    image: UploadFile = File(...),
):
    try:
        
        # Read image file
        image_bytes = await image.read()
        
        # Process image with YOLO
        detections, annotated_image = process_image(image_bytes)
        print(detections)
        # Check if any objects were detected
        if detections:
            return DetectionResponse(
                detection_id="detection_id",
                timestamp="timestamp.isoformat()",
                objects_detected=detections,
                response_image = annotated_image
            )
        else:
            # Save results to database
            # detection_id, timestamp = save_to_db(image_bytes, data, [])
            return DetectionResponse(
                detection_id="none",
                timestamp=datetime.now().isoformat(),
                objects_detected=[],
                response_image = ""
            )
            
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="Invalid JSON data")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Run with: uvicorn main:app --reload
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)