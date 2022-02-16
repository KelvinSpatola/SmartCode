## How to install SmartCode Mode


### Install with the Contribution Manager

Add contributed mode by selecting the menu item _Tools_ → _Add Mode..._ This will open the Contribution Manager, where you can browse for SmartCode Mode, or any other mode you want to install.

Not all available mode have been converted to show up in this menu. If a mode isn't there, it will need to be installed manually by following the instructions below.

### Manual Install

Contributed modes may be downloaded separately and manually placed within the `modes` folder of your Processing sketchbook. To find (and change) the Processing sketchbook location on your computer, open the Preferences window from the Processing application (PDE) and look for the "Sketchbook location" item at the top.

By default the following locations are used for your sketchbook folder: 
  * For Mac users, the sketchbook folder is located inside `~/Documents/Processing` 
  * For Windows users, the sketchbook folder is located inside `My Documents/Processing`

Download SmartCode Mode from https://github.com/KelvinSpatola/SmartCode

Unzip and copy the contributed mode's folder into the `modes` folder in the Processing sketchbook. You will need to create this `modes` folder if it does not exist.
    
The folder structure for mode SmartCode Mode should be as follows:

```
Processing
  modes
    SmartCode Mode
      data
      examples
      mode
        SmartCode Mode.jar
      theme
```
                      
Some folders like `examples` or `data` or some other project specific folders might be missing. After mode SmartCode Mode has been successfully installed, restart the Processing application.

### Troubleshooting

If you're having trouble, try contacting the author [Kelvin Clark Spatola](https://github.com/KelvinSpatola).
