import React, { useState } from 'react';
import LoginCard from '../components/LoginCard';
import SignupCard from '../components/SignUpCard';

function HomeCard() {
  const [showSignup, setShowSignup] = useState(false); // Track if we need to show SignupCard

  const handleToggleSignup = () => {
    setShowSignup(prev => !prev); // Toggle between login and signup
  };

  return (
    <div className="home-card">
      {showSignup ? (
        <SignupCard handleToggleSignup={handleToggleSignup} />
      ) : (
        <LoginCard handleToggleSignup={handleToggleSignup} />
      )}
    </div>
  );
}

export default HomeCard;
