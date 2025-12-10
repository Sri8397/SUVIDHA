import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Features from './components/Features';
import Solutions from './components/Solutions';
import Pricing from './components/Pricing';
import Testimonials from './components/Testimonials';
import Footer from './components/Footer';
import './App.css';
import useUserTracker from './hooks/useUserTracker';

function App() {
  useUserTracker();

  return (
    <div className="App">
      <Navbar />
      <Hero />
      <Features />
      <Solutions />
      <Pricing />
      <Testimonials />
      <Footer />
    </div>
  );
}

export default App;
