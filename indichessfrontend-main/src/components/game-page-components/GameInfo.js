import React from "react";
import { FaFire, FaRegHandshake, FaRobot, FaChessPawn } from "react-icons/fa"; // Importing icons from react-icons
import "../component-styles/GameInfo.css";

const GameInfo = ({ streak }) => {
  return (
    <div className="game-info">
      {/* Streak Section */}
      <div className="streak">
        <FaFire size={30} />
        <div>
          <p>Streak</p>
          <h3>{streak} Days</h3>
        </div>
      </div>

      {/* Buttons Section */}
      <div className="buttons">
        <button className="button">
          <FaChessPawn size={20} />
          Play 1 | 1
        </button>
        <button className="button">
          <FaChessPawn size={20} />
          New Game
        </button>
        <button className="button">
          <FaRobot size={20} />
          Play Bots
        </button>
        <button className="button">
          <FaRegHandshake size={20} />
          Play a Friend
        </button>
      </div>
    </div>
  );
};

export default GameInfo;
