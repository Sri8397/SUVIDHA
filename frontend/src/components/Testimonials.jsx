import { useState } from 'react';
import './Testimonials.css';
import useActivityTracker from '../hooks/useActivityTracker';

const Testimonials = () => {
    const [activeIndex, setActiveIndex] = useState(0);
    const { trackClick } = useActivityTracker();

    const testimonials = [
        {
            id: 1,
            name: "Rajesh Kumar",
            role: "Owner, Kumar Textiles",
            location: "Surat, Gujarat",
            image: "👨‍💼",
            rating: 5,
            text: "Suvidha has transformed how we manage our textile business. The inventory management and GST compliance features have saved us countless hours. Our efficiency has increased by 40%!",
            highlight: "40% efficiency increase"
        },
        {
            id: 2,
            name: "Priya Sharma",
            role: "Founder, Sharma Electronics",
            location: "Delhi",
            image: "👩‍💼",
            rating: 5,
            text: "As a small electronics retailer, managing finances was always a headache. Suvidha's automated invoicing and payment tracking have been game-changers. Highly recommended!",
            highlight: "Automated invoicing"
        },
        {
            id: 3,
            name: "Amit Patel",
            role: "Director, Patel Manufacturing",
            location: "Pune, Maharashtra",
            image: "👨‍🏭",
            rating: 5,
            text: "The best investment we made for our manufacturing unit. Real-time analytics and employee management features are exceptional. Customer support is outstanding too!",
            highlight: "Real-time analytics"
        },
        {
            id: 4,
            name: "Meena Reddy",
            role: "CEO, Reddy Foods",
            location: "Hyderabad, Telangana",
            image: "👩‍🍳",
            rating: 5,
            text: "Suvidha helped us scale from a small food business to multiple outlets. The multi-location support and inventory sync are perfect for our growing needs.",
            highlight: "Multi-location support"
        },
        {
            id: 5,
            name: "Vikram Singh",
            role: "Owner, Singh Auto Parts",
            location: "Jaipur, Rajasthan",
            image: "👨‍🔧",
            rating: 5,
            text: "Managing auto parts inventory was complex until we found Suvidha. The barcode scanning and stock alerts have eliminated stockouts completely. Brilliant solution!",
            highlight: "Zero stockouts"
        },
        {
            id: 6,
            name: "Anjali Desai",
            role: "Founder, Desai Handicrafts",
            location: "Ahmedabad, Gujarat",
            image: "👩‍🎨",
            rating: 5,
            text: "As a handicraft business, we needed something simple yet powerful. Suvidha's user-friendly interface and affordable pricing made it perfect for us. Love it!",
            highlight: "User-friendly & affordable"
        }
    ];

    const nextTestimonial = () => {
        trackClick('testimonials_next_click', { currentTestimonial: testimonials[activeIndex].name, section: 'testimonials' });
        setActiveIndex((prev) => (prev + 1) % testimonials.length);
    };

    const prevTestimonial = () => {
        trackClick('testimonials_prev_click', { currentTestimonial: testimonials[activeIndex].name, section: 'testimonials' });
        setActiveIndex((prev) => (prev - 1 + testimonials.length) % testimonials.length);
    };

    const goToTestimonial = (index) => {
        trackClick('testimonials_dot_click', { testimonialIndex: index + 1, section: 'testimonials' });
        setActiveIndex(index);
    };

    return (
        <section id="testimonials" className="testimonials-section">
            <div className="container">
                <div className="section-header">
                    <span className="section-tag">Success Stories</span>
                    <h2 className="section-title">
                        Trusted by <span className="gradient-text">10,000+</span> MSMEs
                    </h2>
                    <p className="section-description">
                        See how businesses across India are growing with Suvidha
                    </p>
                </div>

                <div className="testimonials-container">
                    <button
                        className="testimonial-nav prev"
                        onClick={prevTestimonial}
                        aria-label="Previous testimonial"
                    >
                        ‹
                    </button>

                    <div className="testimonials-carousel">
                        {testimonials.map((testimonial, index) => {
                            let position = 'hidden';

                            if (index === activeIndex) {
                                position = 'active';
                            } else if (
                                index === activeIndex - 1 ||
                                (activeIndex === 0 && index === testimonials.length - 1)
                            ) {
                                position = 'prev';
                            } else if (
                                index === activeIndex + 1 ||
                                (activeIndex === testimonials.length - 1 && index === 0)
                            ) {
                                position = 'next';
                            }

                            return (
                                <div
                                    key={testimonial.id}
                                    className={`testimonial-card ${position}`}
                                >
                                    <div className="testimonial-content">
                                        <div className="quote-icon">"</div>
                                        <div className="stars">
                                            {[...Array(testimonial.rating)].map((_, i) => (
                                                <span key={i} className="star">★</span>
                                            ))}
                                        </div>
                                        <p className="testimonial-text">{testimonial.text}</p>
                                        <div className="testimonial-highlight">
                                            <span className="highlight-badge">{testimonial.highlight}</span>
                                        </div>
                                    </div>
                                    <div className="testimonial-author">
                                        <div className="author-avatar">{testimonial.image}</div>
                                        <div className="author-info">
                                            <h4 className="author-name">{testimonial.name}</h4>
                                            <p className="author-role">{testimonial.role}</p>
                                            <p className="author-location">📍 {testimonial.location}</p>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>

                    <button
                        className="testimonial-nav next"
                        onClick={nextTestimonial}
                        aria-label="Next testimonial"
                    >
                        ›
                    </button>
                </div>

                <div className="testimonial-dots">
                    {testimonials.map((_, index) => (
                        <button
                            key={index}
                            className={`dot ${index === activeIndex ? 'active' : ''}`}
                            onClick={() => goToTestimonial(index)}
                            aria-label={`Go to testimonial ${index + 1}`}
                        />
                    ))}
                </div>

                <div className="testimonials-stats">
                    <div className="stat-item">
                        <div className="stat-number">10,000+</div>
                        <div className="stat-label">Happy Customers</div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-number">4.9/5</div>
                        <div className="stat-label">Average Rating</div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-number">98%</div>
                        <div className="stat-label">Satisfaction Rate</div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-number">24/7</div>
                        <div className="stat-label">Support Available</div>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default Testimonials;
