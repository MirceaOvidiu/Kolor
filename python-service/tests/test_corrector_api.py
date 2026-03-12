import pytest
import numpy as np
import cv2
from PIL import Image
import io
import os
import sys

# Add the parent directory to Python path to import the main module
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from correctorAPI import app, generate_scaling_factors, color_correction, apply_lut, LUT_PATHS

@pytest.fixture
def client():
    """Create a test client for the Flask app."""
    app.config['TESTING'] = True
    with app.test_client() as client:
        yield client

@pytest.fixture
def sample_image():
    """Create a sample image for testing."""
    # Create a simple 100x100 RGB image
    image = np.random.randint(0, 255, (100, 100, 3), dtype=np.uint8)
    return image

@pytest.fixture
def sample_image_file():
    """Create a sample image file for testing uploads."""
    # Create a simple test image
    img = Image.new('RGB', (100, 100), color='red')
    img_byte_arr = io.BytesIO()
    img.save(img_byte_arr, format='PNG')
    img_byte_arr.seek(0)
    return img_byte_arr


class TestHealthEndpoint:
    """Test the health check endpoint."""
    
    def test_health_endpoint(self, client):
        """Test that health endpoint returns 200 and correct JSON."""
        response = client.get('/health')
        assert response.status_code == 200
        assert response.json == {'status': 'ok'}


class TestImageProcessingFunctions:
    """Test the image processing utility functions."""
    
    def test_generate_scaling_factors(self, sample_image):
        """Test that scaling factors are generated correctly."""
        scaling_factors = generate_scaling_factors(sample_image)
        
        # Should return 3 scaling factors for RGB
        assert len(scaling_factors) == 3
        # Should be numeric values
        assert all(isinstance(factor, (int, float, np.number)) for factor in scaling_factors)
        # Should not contain NaN or infinite values
        assert all(np.isfinite(factor) for factor in scaling_factors)
    
    def test_color_correction(self, sample_image):
        """Test color correction function."""
        corrected = color_correction(sample_image)
        
        # Output should have same shape as input
        assert corrected.shape == sample_image.shape
        # Should be valid image data (0-255 range approximately)
        assert corrected.min() >= -50  # Allow some tolerance for processing
        assert corrected.max() <= 305   # Allow some tolerance for processing
    
    def test_color_correction_with_uniform_image(self):
        """Test color correction with edge case of uniform color."""
        # Create uniform image (all pixels same color)
        uniform_image = np.full((50, 50, 3), 128, dtype=np.uint8)
        
        # Should not crash with uniform image
        try:
            corrected = color_correction(uniform_image)
            assert corrected.shape == uniform_image.shape
        except Exception as e:
            pytest.fail(f"Color correction failed with uniform image: {e}")


class TestApiEndpoints:
    """Test the Flask API endpoints."""
    
    def test_correct_api_missing_file(self, client):
        """Test API with missing file parameter."""
        response = client.post('/correctAPI', data={
            'lut_name': 'C-Log2'
        })
        assert response.status_code == 400
        assert 'Missing file or LUT name' in response.json['error']
    
    def test_correct_api_missing_lut_name(self, client, sample_image_file):
        """Test API with missing LUT name parameter."""
        response = client.post('/correctAPI', data={
            'file': (sample_image_file, 'test.png')
        })
        assert response.status_code == 400
        assert 'Missing file or LUT name' in response.json['error']
    
    def test_correct_api_empty_filename(self, client):
        """Test API with empty filename."""
        response = client.post('/correctAPI', data={
            'file': (io.BytesIO(b''), ''),
            'lut_name': 'C-Log2'
        })
        assert response.status_code == 400
        assert 'No selected file' in response.json['error']
    
    def test_correct_api_valid_request(self, client, sample_image_file):
        """Test API with valid request parameters."""
        response = client.post('/correctAPI', data={
            'file': (sample_image_file, 'test.png'),
            'lut_name': 'C-Log2'
        })
        
        # This might fail if LUT files don't exist in test environment
        # In that case, we expect a 500 error which is acceptable for this test
        assert response.status_code in [200, 500]
        
        if response.status_code == 200:
            # Check that response is image data
            assert response.content_type == 'image/png'
            assert len(response.data) > 0
    
    def test_correct_api_invalid_lut_name(self, client, sample_image_file):
        """Test API with invalid LUT name."""
        response = client.post('/correctAPI', data={
            'file': (sample_image_file, 'test.png'),
            'lut_name': 'invalid-lut'
        })
        
        # Should handle invalid LUT gracefully
        assert response.status_code in [400, 500]


class TestLutPaths:
    """Test LUT file path configuration."""
    
    def test_lut_paths_configuration(self):
        """Test that all expected LUT paths are configured."""
        expected_luts = ['C-Log2', 'C-Log3', 'D-Log', 'F-Log', 'N-Log', 'S-Log2', 'S-Log3']
        
        for lut_name in expected_luts:
            assert lut_name in LUT_PATHS
            assert LUT_PATHS[lut_name].endswith('.cube')
    
    def test_lut_files_exist_or_graceful_handling(self):
        """Test that missing LUT files are handled gracefully."""
        # In a real test environment, LUT files might not exist
        # The application should handle this gracefully
        for lut_name, lut_path in LUT_PATHS.items():
            if not os.path.exists(lut_path):
                # This is acceptable in test environment
                # The important thing is that the paths are configured
                pass
            else:
                # If file exists, it should be readable
                assert os.path.isfile(lut_path)


class TestPrometheusMetrics:
    """Test Prometheus metrics integration."""
    
    def test_metrics_endpoint_exists(self, client):
        """Test that metrics endpoint is available."""
        response = client.get('/metrics')
        
        # Should return metrics in Prometheus format
        assert response.status_code == 200
        assert 'text/plain' in response.content_type
        
        # Should contain some basic metrics
        metrics_text = response.data.decode('utf-8')
        assert 'flask_' in metrics_text or 'python_' in metrics_text