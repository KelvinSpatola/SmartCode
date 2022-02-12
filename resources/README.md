## How to install @@name@@


### Install with the Contribution Manager

Add contributed mode by selecting the menu item _Tools_ → _Add Mode..._ This will open the Contribution Manager, where you can browse for @@name@@, or any other mode you want to install.

Not all available mode have been converted to show up in this menu. If a mode isn't there, it will need to be installed manually by following the instructions below.

### Manual Install

Contributed modes may be downloaded separately and manually placed within the `modes` folder of your Processing sketchbook. To find (and change) the Processing sketchbook location on your computer, open the Preferences window from the Processing application (PDE) and look for the "Sketchbook location" item at the top.

By default the following locations are used for your sketchbook folder: 
  * For Mac users, the sketchbook folder is located inside `~/Documents/Processing` 
  * For Windows users, the sketchbook folder is located inside `My Documents/Processing`

Download @@name@@ from @@project.url@@

Unzip and copy the contributed mode's folder into the `modes` folder in the Processing sketchbook. You will need to create this `modes` folder if it does not exist.
    
The folder structure for mode @@name@@ should be as follows:

```
Processing
  modes
    @@name@@
      data
      examples
      mode
        @@name@@.jar
      theme
```
                      
Some folders like `examples` or `data` or some other project specific folders might be missing. After mode @@name@@ has been successfully installed, restart the Processing application.

### Troubleshooting

If you're having trouble, try contacting the author [@@author.name@@](@@author.url@@).
