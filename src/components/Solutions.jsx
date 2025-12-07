import './Solutions.css';

const Solutions = () => {
    const solutions = [
        {
            id: 1,
            icon: "🏭",
            title: "Manufacturing",
            description: "Streamline production planning, inventory management, and quality control for manufacturing units.",
            features: [
                "Production scheduling",
                "Raw material tracking",
                "Quality assurance",
                "Vendor management"
            ],
            color: "rgba(99, 102, 241, 0.2)"
        },
        {
            id: 2,
            icon: "🛒",
            title: "Retail & E-commerce",
            description: "Manage multi-channel sales, inventory sync, and customer relationships seamlessly.",
            features: [
                "Point of Sale (POS)",
                "Multi-location inventory",
                "Customer loyalty programs",
                "Online-offline sync"
            ],
            color: "rgba(168, 85, 247, 0.2)"
        },
        {
            id: 3,
            icon: "🍽️",
            title: "Food & Beverage",
            description: "Handle orders, recipes, inventory, and compliance for restaurants and food businesses.",
            features: [
                "Menu management",
                "Recipe costing",
                "Table reservations",
                "FSSAI compliance"
            ],
            color: "rgba(236, 72, 153, 0.2)"
        },
        {
            id: 4,
            icon: "🏗️",
            title: "Construction",
            description: "Track projects, materials, labor, and finances for construction and contracting businesses.",
            features: [
                "Project tracking",
                "Material estimation",
                "Labor management",
                "Progress billing"
            ],
            color: "rgba(251, 146, 60, 0.2)"
        },
        {
            id: 5,
            icon: "💊",
            title: "Healthcare & Pharma",
            description: "Manage patient records, appointments, inventory, and regulatory compliance.",
            features: [
                "Appointment scheduling",
                "Medicine inventory",
                "Patient records",
                "Drug license tracking"
            ],
            color: "rgba(34, 197, 94, 0.2)"
        },
        {
            id: 6,
            icon: "🎓",
            title: "Education",
            description: "Streamline admissions, fee management, attendance, and student performance tracking.",
            features: [
                "Student enrollment",
                "Fee collection",
                "Attendance tracking",
                "Report cards"
            ],
            color: "rgba(59, 130, 246, 0.2)"
        },
        {
            id: 7,
            icon: "🚚",
            title: "Logistics & Transport",
            description: "Optimize fleet management, route planning, and delivery tracking for logistics businesses.",
            features: [
                "Fleet tracking",
                "Route optimization",
                "Delivery management",
                "Fuel monitoring"
            ],
            color: "rgba(14, 165, 233, 0.2)"
        },
        {
            id: 8,
            icon: "✂️",
            title: "Services",
            description: "Manage appointments, customer bookings, and service delivery for service-based businesses.",
            features: [
                "Online booking",
                "Staff scheduling",
                "Service packages",
                "Customer feedback"
            ],
            color: "rgba(217, 70, 239, 0.2)"
        }
    ];

    return (
        <section id="solutions" className="solutions-section">
            <div className="container">
                <div className="section-header">
                    <span className="section-tag">Industry Solutions</span>
                    <h2 className="section-title">
                        Tailored for <span className="gradient-text">Every Industry</span>
                    </h2>
                    <p className="section-description">
                        Purpose-built solutions designed for the unique needs of Indian MSMEs across diverse sectors
                    </p>
                </div>

                <div className="solutions-grid">
                    {solutions.map((solution) => (
                        <div
                            key={solution.id}
                            className="solution-card"
                            style={{ '--card-color': solution.color }}
                        >
                            <div className="solution-icon">{solution.icon}</div>
                            <h3 className="solution-title">{solution.title}</h3>
                            <p className="solution-description">{solution.description}</p>
                            <ul className="solution-features">
                                {solution.features.map((feature, index) => (
                                    <li key={index}>
                                        <span className="feature-check">✓</span>
                                        {feature}
                                    </li>
                                ))}
                            </ul>
                            <button className="solution-btn">
                                Learn More
                                <span className="btn-arrow">→</span>
                            </button>
                        </div>
                    ))}
                </div>

                <div className="solutions-cta">
                    <div className="cta-content">
                        <h3>Don't see your industry?</h3>
                        <p>Our platform is highly customizable. Let's discuss your specific needs.</p>
                    </div>
                    <button className="btn btn-primary">Contact Sales</button>
                </div>
            </div>
        </section>
    );
};

export default Solutions;
