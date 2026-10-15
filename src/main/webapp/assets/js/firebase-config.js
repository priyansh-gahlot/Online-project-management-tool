// TODO: Replace with your Firebase project's web configuration
const firebaseConfig = {
  apiKey: "AIzaSyAFgpVMno5xm_hUVl7Wo177TU17duqN7Sc",
  authDomain: "test1-7da13.firebaseapp.com",
  projectId: "test1-7da13",
  storageBucket: "test1-7da13.firebasestorage.app",
  messagingSenderId: "640873344328",
  appId: "1:640873344328:web:5efa4c335318680ccf177d",
  measurementId: "G-8VL12E5LMG"
};

// Initialize Firebase
if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
}
const auth = firebase.auth();
