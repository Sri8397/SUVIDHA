import './Footer.css';

const Footer = () => {
    return (
        <footer className="footer">
            <div className="footer-top">
                <div className="container">
                    <div className="footer-grid">
                        <div className="footer-brand">
                            <div className="logo">
                                <div className="logo-icon"></div>
                                <div className="logo-text">
                                    <span className="logo-name">SUVIDHA</span>
                                    <span className="logo-tagline">सुविधा</span>
                                </div>
                            </div>
                            <p className="footer-description">
                                Empowering Indian MSMEs with simple, powerful business management tools.
                                Built for India, by Indians.
                            </p>
                            <div className="social-links">
                                <a href="#" aria-label="Facebook">FB</a>
                                <a href="#" aria-label="Twitter">TW</a>
                                <a href="#" aria-label="LinkedIn">IN</a>
                                <a href="#" aria-label="Instagram">IG</a>
                            </div>
                        </div>

                        <div className="footer-links">
                            <h4>Product</h4>
                            <ul>
                                <li><a href="#features">Features</a></li>
                                <li><a href="#pricing">Pricing</a></li>
                                <li><a href="#">Integrations</a></li>
                                <li><a href="#">Mobile App</a></li>
                            </ul>
                        </div>

                        <div className="footer-links">
                            <h4>Solutions</h4>
                            <ul>
                                <li><a href="#">Retail</a></li>
                                <li><a href="#">Manufacturing</a></li>
                                <li><a href="#">Services</a></li>
                                <li><a href="#">Wholesale</a></li>
                            </ul>
                        </div>

                        <div className="footer-links">
                            <h4>Company</h4>
                            <ul>
                                <li><a href="#">About Us</a></li>
                                <li><a href="#">Careers</a></li>
                                <li><a href="#">Blog</a></li>
                                <li><a href="#">Contact</a></li>
                            </ul>
                        </div>

                        <div className="footer-links">
                            <h4>Support</h4>
                            <ul>
                                <li><a href="#">Help Center</a></li>
                                <li><a href="#">Documentation</a></li>
                                <li><a href="#">Privacy Policy</a></li>
                                <li><a href="#">Terms of Service</a></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            <div className="footer-bottom">
                <div className="container">
                    <p>&copy; 2024 Suvidha. All rights reserved. Made with ❤️ in India</p>
                </div>
            </div>
        </footer>
    );
};

export default Footer;
