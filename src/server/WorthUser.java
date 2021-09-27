package server;

public class WorthUser implements User {

        private String nickName;
        private String password;


        public WorthUser (String name, String pssw) {
            this.nickName = name;
            this.password = pssw;
        }

        //json
        public WorthUser() {
            this.nickName = null;
            this.password = null;
        }

        public String getNickName() {
            return this.nickName;
        }

        public String getPassword() {return  this.password; }

    }
