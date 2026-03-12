import numpy as np
import cv2
from PIL import Image
from pillow_lut import load_cube_file
from flask import Flask, request, jsonify, send_file
from bytesbufio import BytesBufferIO as BytesIO
import logging
import os
from prometheus_flask_exporter import PrometheusMetrics

app = Flask(__name__)
logger = logging.getLogger(__name__)

# Initialize Prometheus metrics
metrics = PrometheusMetrics(app)

# Add custom metrics
metrics.info('app_info', 'Application info', version='1.0.0')

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

LUT_PATHS = {
    "C-Log2": os.path.join(BASE_DIR, "LUTs", "Canon C-Log2 to Rec.709 LUT 33x33.cube"),
    "C-Log3": os.path.join(BASE_DIR, "LUTs", "Canon C-Log3 to Rec.709 LUT 33x33.cube"),
    "D-Log": os.path.join(BASE_DIR, "LUTs", "DJI D-Log to Rec.709 LUT 33x33.cube"),
    "F-Log": os.path.join(BASE_DIR, "LUTs", "Fujifilm F-Log to Rec.709 LUT 33x33.cube"),
    "N-Log": os.path.join(BASE_DIR, "LUTs", "Nikon N-Log to Rec.709 LUT 33x33.cube"),
    "S-Log2": os.path.join(BASE_DIR, "LUTs", "Sony S-Log2 to Rec.709 LUT 33x33.cube"),
    "S-Log3": os.path.join(BASE_DIR, "LUTs", "Sony S-Log3 to Rec.709 LUT 33x33.cube")
}
def generate_scaling_factors(image):
    pixels = image.reshape((-1, 3)).astype(float)

    mean_vector = np.mean(pixels, axis=0)
    std_vector = np.std(pixels, axis=0)
    # Avoid divide-by-zero for uniform channels.
    std_vector = np.where(std_vector == 0, 1e-8, std_vector)
    standardized_pixels = (pixels - mean_vector) / std_vector

    covariance_matrix = np.cov(standardized_pixels, rowvar=False)

    eigenvalues, eigenvectors = np.linalg.eig(covariance_matrix)

    idx = np.argsort(eigenvalues)[::-1]
    eigenvalues = eigenvalues[idx]
    eigenvectors = eigenvectors[:, idx]

    principal_components = eigenvectors[:, :3]

    transformed_pixels = standardized_pixels.dot(principal_components)

    denom = np.mean(np.abs(transformed_pixels), axis=0)
    denom = np.where(denom == 0, 1e-8, denom)
    scaling_factors = np.std(transformed_pixels, axis=0) / denom
    scaling_factors = np.nan_to_num(scaling_factors, nan=1.0, posinf=1.0, neginf=1.0)

    return scaling_factors

def color_correction(image):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    
    pixels = image.reshape((-1, 3)).astype(float)
    
    mean_vector = np.mean(pixels, axis=0)

    std_vector = np.std(pixels, axis=0)
    std_vector = np.where(std_vector == 0, 1e-8, std_vector)

    standardized_pixels = (pixels - mean_vector) / std_vector

    covariance_matrix = np.cov(standardized_pixels, rowvar=False)

    eigenvalues, eigenvectors = np.linalg.eig(covariance_matrix)

    idx = np.argsort(eigenvalues)[::-1]
    eigenvalues = eigenvalues[idx]
    eigenvectors = eigenvectors[:, idx]
    
    principal_components = eigenvectors[:, :3]

    projected_pixels = np.dot(standardized_pixels, principal_components)

    scaling_factors = generate_scaling_factors(image)

    corrected_pixels = projected_pixels * scaling_factors

    corrected_pixels = np.dot(corrected_pixels, principal_components.T)

    corrected_pixels = corrected_pixels * std_vector + mean_vector

    corrected_pixels = np.nan_to_num(corrected_pixels, nan=0.0, posinf=255.0, neginf=0.0)
    corrected_image = np.clip(corrected_pixels.reshape(image.shape), 0, 255).astype(np.uint8)

    return corrected_image

def apply_lut(image_path, lut_path):
    lut = load_cube_file(lut_path)
    im = Image.open(image_path)
    
    corrected_image_pil = im.filter(lut)
    corrected_image_np = np.array(corrected_image_pil)
    
    return corrected_image_np
    
@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok'}), 200

@app.route('/correctAPI', methods=['POST'])
def color_correct_image():
    if 'file' not in request.files or 'lut_name' not in request.form:
        return jsonify({'error': 'Missing file or LUT name'}), 400

    file = request.files['file']
    lut_name = request.form['lut_name']

    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400

    if file:
        try:
            lut_path = LUT_PATHS.get(lut_name)
            if not lut_path:
                return jsonify({'error': 'Invalid LUT name'}), 400
            
            image_np = apply_lut(file, lut_path)
            if image_np is None:
                logger.error("Failed to apply LUT")
                return jsonify({"error": "Failed to apply LUT."}), 500
            
            corrected_image = color_correction(image_np)
            _, img_encoded = cv2.imencode('.png', corrected_image)

            img_io = BytesIO(img_encoded.tobytes())
            img_io.seek(0)
            
            return send_file(img_io, mimetype='image/png')

        except Exception as e:
            logger.exception("Failed to process image")
            return jsonify({'error': f'Error processing image: {e}'}), 500

    return jsonify({'error': 'No file uploaded'}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)